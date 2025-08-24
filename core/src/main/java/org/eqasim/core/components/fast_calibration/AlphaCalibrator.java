package org.eqasim.core.components.fast_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AlphaCalibrator implements FastCalibration {
    // This class tracks the mode share at the beginning of each iteration and calibrates the alpha value
    // based on the mode share from the previous iteration and the target mode shares.

    private static final Logger logger = LogManager.getLogger(AlphaCalibrator.class);

    private final Map<String, Double> targetModeShares;
    private final double beta;
    private final Scenario scenario;
    private final Map<String, Double> shares = new HashMap<>();
    private final Set<String> consideredModes = Set.of("car", "pt", "walk", "bike", "car_passenger");

    private final OutputDirectoryHierarchy outputHierarchy;
    private final ModeParameters modeParameters;
    private final TripListConverter tripListConverter;
    private int replannedTripsCount = 0;
    private int changedUtilityCount = 0;
    private final IdMap<Person, Double> utilities = new IdMap<>(Person.class);

    public AlphaCalibrator(Scenario scenario,
                           OutputDirectoryHierarchy outputHierarchy,
                           ModeParameters modeParameters,
                           TripListConverter tripListConverter,
                           Map<String, Double> targetModeShares,
                           double beta) {

        this.targetModeShares = targetModeShares;
        this.scenario = scenario;
        this.outputHierarchy = outputHierarchy;
        this.modeParameters = modeParameters;
        this.tripListConverter = tripListConverter;
        this.beta = beta;

        checkTargetModeShares();
        resetPlansCreationFlag();
    }

    public void checkTargetModeShares() {
        // Check first that the required modes are present in the target mode shares
        Set<String> modesWithRequiredShares = Set.of("car", "pt", "walk", "bike");
        for (String mode : targetModeShares.keySet()) {
            if (!modesWithRequiredShares.contains(mode)) {
                logger.warn("Mode '" + mode + "' is not considered in the calibration. This may lead to unexpected results in the calibration process. " +
                        "Please check the target mode shares in the configuration file and ensure you provide shares for all required modes: " + modesWithRequiredShares);
            }
        }
        // ensure the sum of target mode shares is below 1.0 (if passenger_car is not given, else it is 1.0)
        double sumOfShares = targetModeShares.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sumOfShares > 1.0) {
            logger.warn("The sum of target mode shares is greater than 1.0: " + sumOfShares + ". This may lead to unexpected results in the calibration process. " +
                        "Please check the target mode shares in the configuration file.");
        } else if ((sumOfShares < 0.99)&&targetModeShares.containsKey("car_passenger")) {
            logger.warn("The sum of target mode shares is less than 1.0: " + sumOfShares + ". This may lead to unexpected results in the calibration process. " +
                    "Please check the target mode shares in the configuration file.");
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        int iteration = event.getIteration();
        // if a plan is replanned in the last iteration, the createdLastIteration attribute is set to true
        resetPlansCreationFlag();
        // Starting calibration at the beginning of the iteration
        if (iteration>1) {
            updateShares();
            updateAlphas(iteration);
        }
        // Save the shares and alphas to files at the end of each iteration
        saveSharesToFile(iteration);
        saveAlphasToFile(iteration);
    }

    private void resetPlansCreationFlag() {
        changedUtilityCount = 0; // Counter for checking consistency when estimating mode shares
        double epsilon = 0.001102; // some small random value to capture plans that were replanned, but sill same mode and same utility

        for (Person person : scenario.getPopulation().getPersons().values()) {
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
        Map<String, Double> estimatedShares = getEstimatedModeSharesFromPlans();
        for (String mode : targetModeShares.keySet()) {
            double replannedTripsShare = estimatedShares.getOrDefault(mode, 0.0); //0.0 because maybe no trip was recorded for this mode
            shares.put(mode, replannedTripsShare);
        }
    }

    private Map<String, Double> getEstimatedModeSharesFromPlans(){
        // This method estimates the mode shares from the plans created in the last iteration
        // and updates the modeShareTracker with these values.
        Map<String, Double> estimatedShares = new HashMap<>();
        replannedTripsCount = 0; // Reset the count of replanned plans
        for (Person person : scenario.getPopulation().getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            if ((Boolean) plan.getAttributes().getAttribute("createdLastIteration")) {
                List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(plan);
                for (DiscreteModeChoiceTrip trip : trips) {
                    String mode = trip.getInitialMode();
                    boolean sameLocation = trip.getOriginActivity().getCoord().equals(trip.getDestinationActivity().getCoord());
                    // only consider trips with considered modes and different locations
                    if (consideredModes.contains(mode) && !sameLocation) {
                        estimatedShares.put(mode, estimatedShares.getOrDefault(mode, 0.0) + 1.0);
                        replannedTripsCount += 1; // Count the number of replanned plans
                    }
                }
            }
        }

        // Normalize the shares
        double total = estimatedShares.values().stream().mapToDouble(Double::doubleValue).sum()+1e-10; // Avoid division by zero
        for (String mode : estimatedShares.keySet()) {
            double share = estimatedShares.get(mode) / total;
            estimatedShares.put(mode, share);
        }

        if (replannedTripsCount>changedUtilityCount){
            logger.warn("The number of replanned trips (as estimated: " + replannedTripsCount + ") does not match " +
                        "the number of trips in the plans whose utilities have changed (" + changedUtilityCount + "). " +
                        "This might indicate an inconsistency in the calibration process. Please check this part of the code.");
        }

        return estimatedShares;
    }

    private void updateAlphas(int iteration) {
        Map<String, Double> alphas = getAlphas();
        Map<String, Double> newAlphas = new HashMap<>();
        double epsilon = 0.0001; // Small value to avoid log(0) issues
        // Reference mode is pt, its alpha remains unchanged, 0.0
        double mo = Math.max(shares.get("pt"), epsilon);
        double zo = Math.max(targetModeShares.get("pt"), epsilon);
        newAlphas.put("pt", 0.0);
        // update alphas for other modes (car, walk, bike)
        for (String mode : new String[] {"car", "walk", "bike"}) {
            double mi = Math.max(shares.get(mode), epsilon);
            double zi = Math.max(targetModeShares.get(mode), epsilon);
            double alpha = alphas.get(mode);
            // Calculate the new alpha value based on the current share and the target share
            double newAlpha = alpha + (Math.log(zi)-Math.log(mi)) - (Math.log(zo)-Math.log(mo));
            // update it using EMA
            if (Math.abs(newAlpha - alpha) > 1e-3) { // Only use EMA if the change is significant
                double effectiveBeta = Math.min(0.99, beta + (0.99 - beta) * (1.0 - 1.0 / (0.2*iteration + 1.0)));
                newAlpha = effectiveBeta * alpha + (1.0 - effectiveBeta) * newAlpha;
            }
            // put the new alpha in the map
            newAlphas.put(mode, newAlpha);
        }
        // Update the alphas in the mode parameters
        setAlphas(newAlphas);
    }

    private Map<String, Double> getAlphas() {
        Map<String, Double> alphas = new HashMap<>();
        alphas.put("car", modeParameters.car.alpha_u);
        alphas.put("pt", modeParameters.pt.alpha_u);
        alphas.put("walk", modeParameters.walk.alpha_u);
        alphas.put("bike", modeParameters.bike.alpha_u);
        return alphas;
    }

    private void setAlphas(Map<String, Double> alphas) {
        this.modeParameters.car.alpha_u = alphas.get("car");
        this.modeParameters.pt.alpha_u = alphas.get("pt");
        this.modeParameters.walk.alpha_u = alphas.get("walk");
        this.modeParameters.bike.alpha_u = alphas.get("bike");
    }

    private void saveAlphasToFile(int iteration) {
        File outputFile = new File(outputHierarchy.getIterationFilename(iteration, "alphas.csv"));
        Map<String, Double> alphas = getAlphas();
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.println("mode,alpha");
            for (Map.Entry<String, Double> entry : alphas.entrySet()) {
              writer.printf("%s,%.4f%n", entry.getKey(), entry.getValue());
            }
        } catch (IOException e) {
          throw new RuntimeException("Error writing alphas to file: " + outputFile.getAbsolutePath(), e);
        }
    }

    private void saveSharesToFile(int iteration) {
        File outputFile = new File(outputHierarchy.getIterationFilename(iteration, "shares.csv"));
        try (PrintWriter writer = new PrintWriter(outputFile)) {
            writer.println("mode,share,formula_share,tracker_share,replanned_trips_count,changed_utility_count");
            for (Map.Entry<String, Double> entry : shares.entrySet()) {
                String mode = entry.getKey();
                writer.printf("%s,%.4f,%d,%d%n", mode, entry.getValue(), replannedTripsCount, changedUtilityCount);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing shares to file: " + outputFile.getAbsolutePath(), e);
        }
    }

}
