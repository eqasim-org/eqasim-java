package org.eqasim.switzerland.ch_cmdp.calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.fast_calibration.AlphaCalibrationUtils;
import org.eqasim.core.components.fast_calibration.FastCalibration;
import org.eqasim.switzerland.ch_cmdp.mode_choice.parameters.SwissCmdpModeParameters;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;

import java.io.*;
import java.util.*;

public class AlphaClusterCalibrator implements FastCalibration {
    // Logging
    private static final Logger logger = LogManager.getLogger(AlphaClusterCalibrator.class);

    // MATSim and scenario-related
    private final Scenario scenario;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final SwissCmdpModeParameters modeParameters;
    private final TripListConverter tripListConverter;
    private final int lastIteration;

    // Calibration parameters
    private final double beta;
    private final boolean isActivated;
    private final String modeSharesFile;

    // Mode and cluster data
    private final List<String> modesToCalibrate;
    private final Set<String> consideredModes = Set.of("car", "pt", "walk", "bike", "car_passenger");

    // Mode share and counts
    private final Map<String, Double> shares = new HashMap<>();
    private final Map<Integer, Map<String, Double>> sharesByCluster = new HashMap<>();

    private final Map<String, Double> modeCounts = new HashMap<>();
    private final Map<Integer, Map<String, Double>> modeCountsByCluster = new HashMap<>();

    private final Map<String, Double> targetModeShares;
    private final Map<Integer, Map<String, Double>> targetModeSharesByCluster;

    // Utility and statistics
    @SuppressWarnings("null")
    private final IdMap<Person, Double> utilities = new IdMap<>(Person.class);
    private int replannedTripsCount = 0;
    private int changedUtilityCount = 0;
    private int numGlobalUpdates = 0;

    // track changes of region 0
    private final Map<String, Double> alphasRegion0Changes = new HashMap<String, Double>();

    public AlphaClusterCalibrator(Scenario scenario,
                                  OutputDirectoryHierarchy outputHierarchy,
                                  Map<String, Double> targetModeShares,
                                  SwissCmdpModeParameters modeParameters,
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
        this.modeSharesFile = filePath;
        this.lastIteration = scenario.getConfig().controller().getLastIteration();

        if (isActivated) {
            // assert if file exists
            assertIfFileExists();
            // read the target mode shares by canton from the file
            this.targetModeSharesByCluster = readClustersModeShares();
            resetPlansCreationFlag();
        } else {
            this.targetModeSharesByCluster = new HashMap<>();
        }
        logger.info("AlphaCantonCalibrator initialized.");
    }

    private void assertIfFileExists() {
        File file = new File(modeSharesFile);
        if (!file.exists()) {
            throw new IllegalArgumentException("Cantons mode share file does not exist: " + modeSharesFile);
        }
    }

    private Map<Integer, Map<String, Double>> readClustersModeShares() {
        Map<Integer, Map<String, Double>> result = new HashMap<>();
        File file = new File(modeSharesFile);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                logger.warn("Clusters mode share file has no header");
                return result;
            }

            String[] headers = headerLine.split(",");
            // headers: cluster,car,pt,walk,bike,car_passenger
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(",");
                int cluster = Integer.parseInt(tokens[0]);;
                Map<String, Double> modeShares = new HashMap<>();
                for (int i = 1; i < headers.length; i++) {
                    modeShares.put(headers[i], Double.parseDouble(tokens[i]));
                }
                result.put(cluster, modeShares);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading cantons mode shares file: " + file.getAbsolutePath(), e);
        }
        return result;
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

    private void updateCounts(){
        replannedTripsCount = 0; // Reset the count of replanned plans
        for (Person person : scenario.getPopulation().getPersons().values()) {
            if (!AlphaCalibrationUtils.isConsideredPerson(person)) {
                continue; // Skip cross-border and freight persons
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

                        int cluster = SwissPredictorUtils.getCluster(person);
                        Map<String, Double> clusterModeCounts = modeCountsByCluster.computeIfAbsent(cluster, k -> new HashMap<>());
                        clusterModeCounts.put(mode, clusterModeCounts.getOrDefault(mode, 0.0) + 1.0);
                        replannedTripsCount += 1; // Count the number of replanned plans
                    }
                }
            }
        }
        logger.info("Replanned trips count: {} | Changed utility count: {}", replannedTripsCount, changedUtilityCount);
        if (replannedTripsCount>changedUtilityCount) {
            logger.warn("The number of replanned trips is greater than the number of changed utilities. This may indicate an issue in the code.");
        }
    }

    private void resetCounts() {
        for (int cluster : targetModeSharesByCluster.keySet()){
            modeCountsByCluster.put(cluster, new HashMap<>()); // reset the counts for this cluster
        }
        modeCounts.clear();
    }

    private void updateShares() {
        Map<String, Double> estimatedGlobalShares = new HashMap<>();
        double total = modeCounts.values().stream().mapToDouble(Double::doubleValue).sum()+1e-10; // Avoid division by zero
        for (String mode : modeCounts.keySet()) {
            estimatedGlobalShares.put(mode, modeCounts.get(mode) / total);
        }

        Map<Integer, Map<String, Double>> estimatedSharesByCluster = new HashMap<>();
        for (Map.Entry<Integer, Map<String, Double>> entry : modeCountsByCluster.entrySet()) {
            int cluster = entry.getKey();
            Map<String, Double> clusterCounts = entry.getValue();
            Map<String, Double> clusterShares = new HashMap<>();
            double clusterTotal = clusterCounts.values().stream().mapToDouble(Double::doubleValue).sum() + 1e-10; // Avoid division by zero
            for (String mode : clusterCounts.keySet()) {
                double share = clusterCounts.get(mode) / clusterTotal;
                clusterShares.put(mode, share);
            }
            estimatedSharesByCluster.put(cluster, clusterShares);
        }

        for (String mode : consideredModes) {
            double replannedTripsShare = estimatedGlobalShares.getOrDefault(mode, 0.0); //0.0 because maybe no trip was recorded for this mode
            shares.put(mode, replannedTripsShare);
        }

        for (int cluster : estimatedSharesByCluster.keySet()) {
            Map<String, Double> clusterShares = estimatedSharesByCluster.get(cluster);
            Map<String, Double> storedClusterShares = sharesByCluster.computeIfAbsent(cluster, k -> new HashMap<>());

            for (String mode : consideredModes) {
                double replannedTripsShare = clusterShares.getOrDefault(mode, 0.0); //0.0 because maybe no trip was recorded for this mode
                storedClusterShares.put(mode, replannedTripsShare);
            }
        }
    }

    private Map<String, Double> getAlphas(int cluster) {
        Map<String, Double> alphas = new HashMap<>();
        switch (cluster) {
            case 1:
                alphas.put("car", modeParameters.car.betaRegion1_u);
                alphas.put("pt", modeParameters.pt.betaRegion1_u);
                alphas.put("walk", modeParameters.walk.betaRegion1_u);
                alphas.put("bike", modeParameters.bike.betaRegion1_u);
                alphas.put("car_passenger", modeParameters.cp.betaRegion1_u);
                break;
            case 2:
                alphas.put("car", modeParameters.car.betaRegion2_u);
                alphas.put("pt", modeParameters.pt.betaRegion2_u);
                alphas.put("walk", modeParameters.walk.betaRegion2_u);
                alphas.put("bike", modeParameters.bike.betaRegion2_u);
                alphas.put("car_passenger", modeParameters.cp.betaRegion2_u);
                break;
            default:
                alphas.put("car", modeParameters.car.alpha_u);
                alphas.put("pt", modeParameters.pt.alpha_u);
                alphas.put("walk", modeParameters.walk.alpha_u);
                alphas.put("bike", modeParameters.bike.alpha_u);
                alphas.put("car_passenger", modeParameters.cp.alpha_u);
                break;
        }
        return alphas;
    }

    private void setAlphas(int cluster, Map<String, Double> alphas){
        // the trick should be here. Since cluster 0 has no specific parameters, I will set the alphas to the default parameters.
        // but for the others, I need to make adjustments.
        switch (cluster) {
            case 1:
                modeParameters.car.betaRegion1_u  = alphas.get("car") - alphasRegion0Changes.getOrDefault("car", 0.0);
                modeParameters.pt.betaRegion1_u   = alphas.get("pt") - alphasRegion0Changes.getOrDefault("pt", 0.0);
                modeParameters.walk.betaRegion1_u = alphas.get("walk") - alphasRegion0Changes.getOrDefault("walk", 0.0);
                modeParameters.bike.betaRegion1_u = alphas.get("bike") - alphasRegion0Changes.getOrDefault("bike", 0.0);
                modeParameters.cp.betaRegion1_u   = alphas.get("car_passenger") - alphasRegion0Changes.getOrDefault("car_passenger", 0.0);
                break;
            case 2:
                modeParameters.car.betaRegion2_u  = alphas.get("car") - alphasRegion0Changes.getOrDefault("car", 0.0);
                modeParameters.pt.betaRegion2_u   = alphas.get("pt") - alphasRegion0Changes.getOrDefault("pt", 0.0);
                modeParameters.walk.betaRegion2_u = alphas.get("walk") - alphasRegion0Changes.getOrDefault("walk", 0.0);
                modeParameters.bike.betaRegion2_u = alphas.get("bike") - alphasRegion0Changes.getOrDefault("bike", 0.0);
                modeParameters.cp.betaRegion2_u   = alphas.get("car_passenger") - alphasRegion0Changes.getOrDefault("car_passenger", 0.0);
                break;
            default:
                alphasRegion0Changes.put("car", alphas.get("car") - modeParameters.car.alpha_u);
                alphasRegion0Changes.put("pt", alphas.get("pt") - modeParameters.pt.alpha_u);
                alphasRegion0Changes.put("walk", alphas.get("walk") - modeParameters.walk.alpha_u);
                alphasRegion0Changes.put("bike", alphas.get("bike") - modeParameters.bike.alpha_u);
                alphasRegion0Changes.put("car_passenger", alphas.get("car_passenger") - modeParameters.cp.alpha_u);

                modeParameters.car.alpha_u  = alphas.get("car");
                modeParameters.pt.alpha_u   = alphas.get("pt");
                modeParameters.walk.alpha_u = alphas.get("walk");
                modeParameters.bike.alpha_u = alphas.get("bike");
                modeParameters.cp.alpha_u   = alphas.get("car_passenger");
                break;
        }
    }

    private double getEffectiveBeta(int matsimIteration) {
        // For global updates, use a staged beta based on the number of global updates
        if (updateGlobal(matsimIteration)) {
            if (numGlobalUpdates < 3) {
                return 0.3;
            } else if (numGlobalUpdates < 6) {
                return 0.5;
            } else if (numGlobalUpdates < 10) {
                return 0.8;
            } else {
                return 0.95;
            }
        }

        // For regional updates, use beta based on iteration number
        if (matsimIteration >= 120) {
            return 0.99;
        } else if (matsimIteration > 90) {
            return 0.95;
        } else if (matsimIteration < 10) {
            return 0.0;
        } else {
            // Gradually increase beta as iterations progress
            return Math.min(0.99, beta + (0.99 - beta) * (1.0 - 1.0 / (0.08 * (matsimIteration - 10.0) + 1.0)));
        }
    }

    private boolean updateGlobal(int iteration) {
        // update only global parameters in the last 20 iterations
        int iterationLimit = Math.min(60, lastIteration/2);

        if ((iteration%7 == 0) && (iteration<iterationLimit)) {
            numGlobalUpdates = 0; // keep it 0, these global updates are not counted
            return true;
        }
        return iteration>=iterationLimit;
    }

    private void updateRegionalAlphas(int matsimIteration) {
        // Here I know there are only three clusters, I want to start with the 0 because it is a special case.
        for (int cluster : List.of(0, 1, 2)) {
            logger.info("Updating alphas for cluster {}", cluster);
            Map<String, Double> alphas = getAlphas(cluster);
            Map<String, Double> clusterShares = sharesByCluster.get(cluster);
            Map<String, Double> targetClusterShares = targetModeSharesByCluster.get(cluster);

            Map<String, Double> newAlphas = new HashMap<>();
            double epsilon = 0.0001; // Small value to avoid log(0) issues

            // Reference mode is pt, its alpha remains unchanged, 0.0
            double mo = Math.max(clusterShares.get("pt"), epsilon);
            double zo = Math.max(targetClusterShares.get("pt"), epsilon);

            newAlphas.put("pt", 0.0);
            // update alphas for other modes (car, walk, bike)
            for (String mode : modesToCalibrate) {
                if (mode.equals("pt")) {
                    continue; // Skip the reference mode
                }
                double mi = Math.max(clusterShares.get(mode), epsilon);
                double zi = Math.max(targetClusterShares.get(mode), epsilon);
                double alpha = alphas.get(mode);

                // Calculate the new alpha value based on the current share and the target share
                double newAlpha = alpha + (Math.log(zi) - Math.log(mi)) - (Math.log(zo) - Math.log(mo));
                // update it using EMA
                double effectiveBeta = getEffectiveBeta(matsimIteration);
                newAlpha = effectiveBeta * alpha + (1.0 - effectiveBeta) * newAlpha;
                // put the new alpha in the map
                newAlphas.put(mode, newAlpha);
            }
            // Update the alphas in the mode parameters
            setAlphas(cluster, newAlphas);

        }
    }

    private void updateGlobalAlphas(int iteration) {
        Map<String, Double> alphas = getAlphas(0);
        Map<String, Double> newAlphas = new HashMap<>();
        double epsilon = 0.0001; // Small value to avoid log(0) issues
        // Reference mode is pt, its alpha remains unchanged, 0.0
        double mo = Math.max(shares.get("pt"), epsilon);
        double zo = Math.max(targetModeShares.get("pt"), epsilon);
        newAlphas.put("pt", 0.0);
        // update alphas for other modes
        for (String mode : modesToCalibrate) {
            if (mode.equals("pt")) {
                continue; // Skip the reference mode
            }

            double mi = Math.max(shares.get(mode), epsilon);
            double zi = Math.max(targetModeShares.get(mode), epsilon);
            double alpha = alphas.get(mode);
            // Calculate the new alpha value based on the current share and the target share
            double newAlpha = alpha + (Math.log(zi)-Math.log(mi)) - (Math.log(zo)-Math.log(mo));
            // update it using EMA
            double effectiveBeta = getEffectiveBeta(iteration);
            newAlpha = effectiveBeta * alpha + (1.0 - effectiveBeta) * newAlpha;
            // put the new alpha in the map
            newAlphas.put(mode, newAlpha);
        }
        // Update the alphas in the mode parameters
        setAlphas(0, newAlphas);
    }

    private void saveAlphasToFile(int iteration) {
        File outputFile = new File(outputHierarchy.getIterationFilename(iteration, "alphas.csv"));
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.print("cluster");
            for (String mode : consideredModes) {
                writer.print("," + mode);
            }
            writer.println();
            for (int cluster : sharesByCluster.keySet()) {
                writer.print(cluster);
                Map<String, Double> alphas = getAlphas(cluster);
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
            for (int cluster : sharesByCluster.keySet()) {
                writer.print("," + cluster);
            }
            writer.println();
            // Write data rows
            for (String mode : shares.keySet()) {
                writer.print(mode + "," + String.format("%.4f", shares.get(mode)));
                for (int cluster : sharesByCluster.keySet()) {
                    double cantonShare = sharesByCluster.get(cluster).getOrDefault(mode, 0.0);
                    writer.print("," + String.format("%.4f", cantonShare));
                }
                writer.println();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing shares to file: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void saveParametersToYaml(int iteration) throws IOException {
        String outputFile = outputHierarchy.getIterationFilename(iteration, "mode_parameters.yml");
        modeParameters.saveToYamlFile(outputFile);
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
            // update the shares based on the previous counts, and reset the counts
            updateShares();
            // update the alphas based on the updated shares
            if (updateGlobal(iteration)){
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


}
