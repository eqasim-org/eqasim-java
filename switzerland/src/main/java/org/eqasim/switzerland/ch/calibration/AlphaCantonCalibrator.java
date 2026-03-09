package org.eqasim.switzerland.ch.calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.fast_calibration.AlphaCalibrationUtils;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.switzerland.ch.mode_choice.parameters.SwissModeParameters;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.*;
import java.util.*;

public class AlphaCantonCalibrator implements FastCalibration, ShutdownListener {
    // Logging
    private static final Logger logger = LogManager.getLogger(AlphaCantonCalibrator.class);

    // Calibration targets
    private final Map<String, Map<String, Double>> targetModeSharesByCanton;
    private final Map<String, Double> targetModeShares;
    private final List<String> modesToCalibrate;
    private final Set<String> consideredModes = Set.of("car", "pt", "walk", "bike", "car_passenger");

    // Calibration state
    private final Map<String, Double> shares = new HashMap<>();
    private final Map<String, Map<String, Double>> sharesByCanton = new HashMap<>();
    private final Map<String, Double> modeCounts = new HashMap<>();
    private final Map<String, Map<String, Double>> modeCountsByCanton = new HashMap<>();
    private final Map<String, Boolean> doUpdateThisIteration = new HashMap<>();
    private final Map<String, Integer> numberOfUpdates = new HashMap<>();
    @SuppressWarnings("null")
    private final IdMap<Person, Double> utilities = new IdMap<>(Person.class);
    private int replannedTripsCount = 0;
    private int changedUtilityCount = 0;
    private int numGlobalUpdates = 0;
    private final int lastIteration;

    // Configuration
    private final Set<String> cantons;
    private final double beta;
    private final int batchSizeLimit = 400; // the minimum number of observations before updating the parameters
    private final boolean isActivated;
    private final String cantonsModeShareFile;

    // External dependencies
    private final Scenario scenario;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final SwissModeParameters modeParameters;
    private final TripListConverter tripListConverter;
    private String lastParametersFile = "";

    public AlphaCantonCalibrator(Scenario scenario,
                                 OutputDirectoryHierarchy outputHierarchy,
                                 Map<String, Double> targetModeShares,
                                 SwissModeParameters modeParameters,
                                 TripListConverter tripListConverter,
                                 List<String> modesToCalibrate,
                                 double beta,
                                 String filePath,
                                 boolean isActivated) {

        this.modesToCalibrate = modesToCalibrate;
        this.scenario = scenario;
        this.targetModeShares = targetModeShares;
        this.outputHierarchy = outputHierarchy;
        this.modeParameters = modeParameters;
        this.tripListConverter = tripListConverter;
        this.beta = beta;
        this.isActivated = isActivated;
        this.cantonsModeShareFile = filePath;
        this.lastIteration = scenario.getConfig().controller().getLastIteration();
        if (isActivated) {
            // assert if file exists
            assertIfFileExists();
            // read the target mode shares by canton from the file
            this.targetModeSharesByCanton = readCantonsModeShares();
            this.cantons = new HashSet<>(targetModeSharesByCanton.keySet());
            for (String canton : cantons) doUpdateThisIteration.put(canton, false);
            resetPlansCreationFlag();
            // resetAllAlphasToZeros(); // adding cantons alphas as offset instead.
        } else {
            this.targetModeSharesByCanton = new HashMap<>();
            this.cantons = new HashSet<>();
        }
        logger.info("AlphaCantonCalibrator initialized.");
    }

    private void assertIfFileExists() {
        File file = new File(cantonsModeShareFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("Cantons mode share file does not exist: " + cantonsModeShareFile);
        }
    }

    private boolean doUpdateGlobal(int iteration) {
        // first 6 iterations: update only global parameters
        if (iteration <= 6) {
            numGlobalUpdates = 0; // keep it 0, these global updates are not counted
            return true;
        }
        // update only global parameters in the last iterations
        int iterationLimit = Math.min(80, 3*lastIteration/4);

        if ((iteration%10 == 0) && (iteration<iterationLimit)) {
            numGlobalUpdates = 0; // keep it 0, these global updates are not counted
            return true;
        }
        return iteration>=iterationLimit;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (!isActivated) {
            return;
        }

        int iteration = event.getIteration();
        // if a plan is replanned in the last iteration, the createdLastIteration attribute is set to true
        resetPlansCreationFlag();
        // Starting calibration at the beginning of the iteration
        if (iteration>1) {
            updateCounts();
            // decide which cantons to update based on the counts
            updateCantonsToUpdate();
            // update the shares based on the previous counts, and reset the counts
            updateShares();
            // update the alphas based on the updated shares
            if (doUpdateGlobal(iteration)){
                logger.info("Iteration {}: Updating Global ASCs", iteration);
                updateGlobalAlphas(iteration);
                numGlobalUpdates += 1;
            }else{
                logger.info("Iteration {}: Updating Regional ASCs", iteration);
                updateRegionalAlphas(iteration);
            }
            // reset the counts for the cantons whose parameters were updated
            resetCounts();
            // writing
            saveSharesToFile(iteration);
            saveAlphasToFile(iteration);
            try {
                saveParametersToYaml(iteration);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void resetPlansCreationFlag() {
        changedUtilityCount = 0; // Counter for checking consistency when estimating mode shares
        double epsilon = 0.001102; // some small random value to capture plans that were replanned, but sill same mode and same utility

        for (Person person : scenario.getPopulation().getPersons().values()) {
            @SuppressWarnings("null")
            Plan plan = person.getSelectedPlan();
            plan.getAttributes().putAttribute("createdLastIteration", false);

            // this part is for checking the utilities' changes
            Double utility = (Double) plan.getAttributes().getAttribute("utility");
            Double lastIterationUtility = utilities.get(person.getId());

            if (utility != null && lastIterationUtility != null) { // this happens in the first iteration, but anyway, calibration starts after the first iteration
                if (Math.abs(utility - lastIterationUtility) > 1e-5) {
                    List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);
                    changedUtilityCount += trips.size();
                    plan.getAttributes().putAttribute("createdLastIteration", true);
                }
            }

            double newUtility = utility==null ? 0.0 : Math.round(utility * 1_000_000.0) / 1_000_000.0 + epsilon; // add this to track changes
            plan.getAttributes().putAttribute("utility", newUtility);
            utilities.put(person.getId(), newUtility);
        }
    }

    private void updateShares() {

        Map<String, Double> estimatedGlobalShares = new HashMap<>();
        double total = modeCounts.values().stream().mapToDouble(Double::doubleValue).sum()+1e-10; // Avoid division by zero
        for (String mode : modeCounts.keySet()) {
            estimatedGlobalShares.put(mode, modeCounts.get(mode) / total);
        }

        Map<String, Map<String, Double>> estimatedSharesByCanton = new HashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : modeCountsByCanton.entrySet()) {
            String canton = entry.getKey();
            Map<String, Double> cantonCounts = entry.getValue();
            Map<String, Double> cantonShares = new HashMap<>();
            double cantonTotal = cantonCounts.values().stream().mapToDouble(Double::doubleValue).sum() + 1e-10; // Avoid division by zero
            for (String mode : cantonCounts.keySet()) {
                double share = cantonCounts.get(mode) / cantonTotal;
                cantonShares.put(mode, share);
            }
            estimatedSharesByCanton.put(canton, cantonShares);
        }

        for (String mode : consideredModes) {
            double replannedTripsShare = estimatedGlobalShares.getOrDefault(mode, 0.0); //0.0 because maybe no trip was recorded for this mode
            shares.put(mode, replannedTripsShare);
        }

        for (String canton : estimatedSharesByCanton.keySet()) {
            Map<String, Double> cantonShares = estimatedSharesByCanton.get(canton);
            Map<String, Double> storedCantonShares = sharesByCanton.computeIfAbsent(canton, k -> new HashMap<>());

            for (String mode : consideredModes) {
                double replannedTripsShare = cantonShares.getOrDefault(mode, 0.0); //0.0 because maybe no trip was recorded for this mode
                storedCantonShares.put(mode, replannedTripsShare);
            }
        }
    }

    private void updateCounts(){
        replannedTripsCount = 0; // Reset the count of replanned plans
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (!AlphaCalibrationUtils.isConsideredPerson(person)) {
                continue; // Skip cross-border and freight agents
            }

            @SuppressWarnings("null")
            Plan plan = person.getSelectedPlan();
            if ((Boolean) plan.getAttributes().getAttribute("createdLastIteration")) {
                List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);

                for (DiscreteModeChoiceTrip trip : trips) {
                    String mode = trip.getInitialMode();
                    boolean sameLocation = trip.getOriginActivity().getCoord().equals(trip.getDestinationActivity().getCoord());
                    boolean consideredTrip = AlphaCalibrationUtils.isConsideredTrip(trip);

                    if (consideredTrip && consideredModes.contains(mode) && !sameLocation) {
                        modeCounts.put(mode, modeCounts.getOrDefault(mode, 0.0) + 1.0);

                        String canton = (String) person.getAttributes().getAttribute("cantonName");
                        Map<String, Double> cantonModeCounts = modeCountsByCanton.computeIfAbsent(canton, k -> new HashMap<>());
                        cantonModeCounts.put(mode, cantonModeCounts.getOrDefault(mode, 0.0) + 1.0);
                        replannedTripsCount += 1; // Count the number of replanned plans
                    }
                }
            }
        }
        logger.info("Replanned trips count: {} | Changed utility count: {}", replannedTripsCount, changedUtilityCount);
        if (replannedTripsCount>changedUtilityCount) {
            logger.warn("The number of replanned trips is greater than the number of changed utilities. This may indicate an inconsistency in the data.");
        }
    }

    private void updateCantonsToUpdate(){
        // make sure all cantons are included
        cantons.addAll(modeCountsByCanton.keySet());
        // check canton by canton, if we update canton parameters this iteration
        for (String canton : cantons){
            Map<String, Double> cantonCounts = modeCountsByCanton.getOrDefault(canton, new HashMap<>());
            int total = (int) cantonCounts.values().stream().mapToDouble(Double::doubleValue).sum();
            if (total >= batchSizeLimit) {
                logger.info("Canton {} has enough data ({} trips). Will update alphas.", canton, total);
                doUpdateThisIteration.put(canton, true);
                numberOfUpdates.put(canton, numberOfUpdates.getOrDefault(canton, -1) + 1); // so that the first iteration will be 0
            } else {
                logger.info("Canton {} does not have enough data ({} trips). Will NOT update alphas.", canton, total);
                doUpdateThisIteration.put(canton, false);
            }
        }
    }

    private void resetCounts() {
        for (String canton : doUpdateThisIteration.keySet()){
            if (doUpdateThisIteration.get(canton)) {
                modeCountsByCanton.put(canton, new HashMap<>()); // reset the counts for this canton
            }
        }
        modeCounts.clear();
    }

    private void updateGlobalAlphas(int matsimIteration) {
        Map<String, Double> currentAlphas = getAlphas("all");
        Map<String, Double> newAlphas = estimateOptimalAlphas(currentAlphas, shares, targetModeShares, 0, matsimIteration);
        setAlphas("all", newAlphas);
    }

    private void updateRegionalAlphas(int matsimIteration) {
        for (String canton : sharesByCanton.keySet()) {
            boolean updateThisCanton = doUpdateThisIteration.getOrDefault(canton, false);
            if (updateThisCanton) {
                logger.info("Updating alphas for canton {}", canton);
                Map<String, Double> alphas = getAlphas(canton);
                Map<String, Double> cantonShares = sharesByCanton.get(canton);
                Map<String, Double> targetCantonShares = targetModeSharesByCanton.get(canton);
                int iteration = numberOfUpdates.getOrDefault(canton, 0);
                // Estimate new alphas
                Map<String, Double> newAlphas = estimateOptimalAlphas(alphas, cantonShares, targetCantonShares, iteration, matsimIteration);
                // Update the alphas in the mode parameters
                setAlphas(canton, newAlphas);
            }
        }
    }

    private Map<String, Double> estimateOptimalAlphas(Map<String, Double> currentAlphas,
                                                      Map<String, Double> estimatedShares,
                                                      Map<String, Double> targetShares,
                                                      int iteration,
                                                      int matsimIteration) {
        Map<String, Double> newAlphas = new HashMap<>();
        double epsilon = 0.0001; // Small value to avoid log(0) issues

        // Reference mode is pt, its alpha remains unchanged, 0.0
        double mo = Math.max(estimatedShares.get("pt"), epsilon);
        double zo = Math.max(targetShares.get("pt"), epsilon);
        newAlphas.put("pt", 0.0);

        // update alphas for other modes
        for (String mode : modesToCalibrate) {
            if (mode.equals("pt")) {
                continue; // Skip the reference mode
            }

            double mi = Math.max(estimatedShares.get(mode), epsilon);
            double zi = Math.max(targetShares.get(mode), epsilon);
            double alpha = currentAlphas.get(mode);

            // Calculate the new alpha value based on the current share and the target share
            double newAlpha = alpha + (Math.log(zi) - Math.log(mi)) - (Math.log(zo) - Math.log(mo));

            // update it using EMA
            double effectiveBeta = getEffectiveBeta(iteration, matsimIteration);
            newAlpha = effectiveBeta * alpha + (1.0 - effectiveBeta) * newAlpha;

            // put the new alpha in the map
            newAlphas.put(mode, newAlpha);
        }
        return newAlphas;
    }

    private double getEffectiveBeta(int iteration, int matsimIteration) {
        // For global updates, use a staged beta based on the number of global updates
        if (doUpdateGlobal(matsimIteration)) {
            if (numGlobalUpdates < 3) {
                return 0.2;
            } else if (numGlobalUpdates < 8) {
                return 0.5;
            } else if (numGlobalUpdates < 12) {
                return 0.8;
            } else {
                return 0.95;
            }
        }

        // For regional updates, use beta based on iteration number
        if (matsimIteration >= 120) {
            return 0.98;
        } else if (matsimIteration > 90) {
            return 0.95;
        } else if (matsimIteration < 20 || iteration < 5) {
            return 0.1;
        } else {
            // Gradually increase beta as iterations progress
            return Math.min(0.99, beta + (0.99 - beta) * (1.0 - 1.0 / (0.1 * iteration + 1.0)));
        }
    }

    private Map<String, Double> getAlphas(String canton) {
        if (canton.equals("all")) {
            return modeParameters.getASCs();
        }
        Map<String, Double> alphas = new HashMap<>();
        alphas.put("car", modeParameters.swissCanton.car.getOrDefault(canton, 0.0));
        alphas.put("pt", modeParameters.swissCanton.pt.getOrDefault(canton, 0.0));
        alphas.put("walk", modeParameters.swissCanton.walk.getOrDefault(canton, 0.0));
        alphas.put("bike", modeParameters.swissCanton.bike.getOrDefault(canton, 0.0));
        alphas.put("car_passenger", modeParameters.swissCanton.cp.getOrDefault(canton, 0.0));
        return alphas;
    }

    private void setAlphas(String canton, Map<String, Double> alphas) {
        if (canton.equals("all")) {
            modeParameters.setASCs(alphas);
        } else {
            if (alphas.containsKey("car")) {
                modeParameters.swissCanton.car.put(canton, alphas.get("car"));
            }
            if (alphas.containsKey("pt")) {
                modeParameters.swissCanton.pt.put(canton, alphas.get("pt"));
            }
            if (alphas.containsKey("walk")) {
                modeParameters.swissCanton.walk.put(canton, alphas.get("walk"));
            }
            if (alphas.containsKey("bike")) {
                modeParameters.swissCanton.bike.put(canton, alphas.get("bike"));
            }
            if (alphas.containsKey("car_passenger")) {
                modeParameters.swissCanton.cp.put(canton, alphas.get("car_passenger"));
            }
        }
    }

    private void saveAlphasToFile(int iteration) {
        File outputFile = new File(outputHierarchy.getIterationFilename(iteration, "alphas.csv"));
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.print("canton");
            for (String mode : consideredModes) {
                writer.print("," + mode);
            }
            writer.println();
            for (String canton : sharesByCanton.keySet()) {
                writer.print(canton);
                Map<String, Double> alphas = getAlphas(canton);
                for (String mode : consideredModes) {
                    writer.print("," + String.format("%.4f", alphas.getOrDefault(mode, 0.0)));
                }
                writer.println();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing alphas to file: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void saveSharesToFile(int iteration) {
        File outputFile = new File(outputHierarchy.getIterationFilename(iteration, "shares.csv"));
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            // Write header: mode, share, then each canton
            writer.print("mode,share");
            for (String canton : sharesByCanton.keySet()) {
                writer.print("," + canton);
            }
            writer.println();
            // Write data rows
            for (String mode : shares.keySet()) {
                writer.print(mode + "," + String.format("%.4f", shares.get(mode)));
                for (String canton : sharesByCanton.keySet()) {
                    double cantonShare = sharesByCanton.get(canton).getOrDefault(mode, 0.0);
                    writer.print("," + String.format("%.4f", cantonShare));
                }
                writer.println();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing shares to file: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void saveParametersToYaml(int iteration) throws IOException {
        lastParametersFile = outputHierarchy.getIterationFilename(iteration, "mode_parameters.yml");
        modeParameters.saveToYamlFile(lastParametersFile);
    }

    private Map<String, Map<String, Double>> readCantonsModeShares() {
        Map<String, Map<String, Double>> result = new HashMap<>();
        File file = new File(cantonsModeShareFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.warn("Cantons mode share file has no header");
                return result;
            }

            String[] headers = headerLine.split(",");
            // headers: canton,car,pt,walk,bike,car_passenger
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                String canton = tokens[0];
                Map<String, Double> modeShares = new HashMap<>();
                for (int i = 1; i < headers.length; i++) {
                    modeShares.put(headers[i], Double.parseDouble(tokens[i]));
                }
                result.put(canton, modeShares);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading cantons mode shares file: " + file.getAbsolutePath(), e);
        }
        return result;
    }

    @Override
    public void notifyShutdown(ShutdownEvent shutdownEvent) {
        // copy the last parameters file to the main output directory
        if (isActivated && !lastParametersFile.isEmpty()) {
            File sourceFile = new File(lastParametersFile);
            File destFile = new File(outputHierarchy.getOutputFilenameWithOutputPrefix("mode_parameters.yml"));
            try {
                java.nio.file.Files.copy(sourceFile.toPath(), destFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new RuntimeException("Error copying mode parameters file to main output directory.", e);
            }
        }
    }
}
