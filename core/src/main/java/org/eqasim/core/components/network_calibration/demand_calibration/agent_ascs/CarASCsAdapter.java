package org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs;

import com.google.inject.Provider;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.util.List;

public class CarASCsAdapter implements IterationEndsListener {

    // --- Softened hyperparameters ---
    private static final double LEARNING_RATE = 2.5;
    private static final double MAX_PERSON_ASC_STEP = 0.5;
    private static final double MIN_PERSON_ASC_STEP = 0.05;
    private static final double MAX_PERSON_ASC = 3.0;
    private static final int WARMUP_ITERATIONS = 20;
    private static final double LEARNING_RATE_DECAY = 0.983;

    private final OutputDirectoryHierarchy outputHierarchy;
    private final Population population;
    private final PopulationGroups populationGroups;
    private final TripListConverter tripListConverter;
    private final ODErrors odErrors;
    private final double dmcWeight;
    private final boolean calibrationEnabled;
    private final double initialLearningRate;
    private int numUpdates;

    public CarASCsAdapter(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, TripListConverter tripListConverter,
                          OutputDirectoryHierarchy outputHierarchy, Provider<ODErrors> odErrorsProvider, double dmcWeight, NetworkCalibrationConfigGroup calConfig) {
        this.population = scenario.getPopulation();
        this.tripListConverter = tripListConverter;
        this.dmcWeight = dmcWeight;
        this.calibrationEnabled = calConfig.getAllObjectives().contains("agent") && calConfig.isCalibrationEnabled();
        this.outputHierarchy = outputHierarchy;
        this.initialLearningRate = LEARNING_RATE;
        this.numUpdates = 0;

        this.odErrors = calibrationEnabled ? odErrorsProvider.get():null;;
        this.populationGroups = calibrationEnabled ? populationGroupsProvider.get():null;

        if (calibrationEnabled) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                if (!Tools.isInSubPopulation(person) && Tools.isCarAvailable(person)) {
                    Tools.setCarASC(person, 0.0);
                }
            }
        }
    }

    public void updateASCs(int iteration) {
        double[][] odCorrections = odErrors.getODCorrections();
        double currentLearningRate = currentLearningRate(iteration);

        for (Person person : population.getPersons().values()) {
            if (Tools.isInSubPopulation(person) || !Tools.isCarAvailable(person)) {
                continue;
            }

            List<DiscreteModeChoiceTrip> trips = tripListConverter.convert(person.getSelectedPlan());

            double personDeltaSum = 0.0;
            int validTrips = 0;

            for (DiscreteModeChoiceTrip trip : trips) {
                Coord origin = trip.getOriginActivity().getCoord();
                Coord destination = trip.getDestinationActivity().getCoord();
                int groupOrigin = populationGroups.getGroup(origin);
                int groupDestination = populationGroups.getGroup(destination);

                if (groupOrigin < 0 || groupDestination < 0
                    || groupOrigin >= odCorrections.length
                    || groupDestination >= odCorrections[groupOrigin].length) {
                    continue;
                }

                personDeltaSum += getDeltaAsc(odCorrections[groupOrigin][groupDestination], currentLearningRate);
                validTrips++;
            }

            if (validTrips > 0) {
                double avgDelta = personDeltaSum / validTrips;
                if (Math.abs(avgDelta)<5e-3){
                    avgDelta = 0.0;
                } else {
                    double magnitude = Math.min(MAX_PERSON_ASC_STEP,
                            Math.max(MIN_PERSON_ASC_STEP, Math.abs(avgDelta)));
                    avgDelta = Math.copySign(magnitude, avgDelta);
                }

                Tools.incrementCarASC(person, avgDelta, MAX_PERSON_ASC);
            }
        }
    }

    private double getDeltaAsc(double odCorrection, double currentLearningRate) {
        return currentLearningRate * odCorrection;
    }

    private double currentLearningRate(int iteration) {
        int effectiveIteration = Math.max(0, iteration - WARMUP_ITERATIONS);
        return Math.max(0.5, initialLearningRate * Math.pow(LEARNING_RATE_DECAY, effectiveIteration));
    }

    private void rebuildPopulationGroupsIfRequired(){
        if (numUpdates==2) {
            populationGroups.reBuild(10_000, 1250, 1000);
        }

        if (numUpdates==4) {
            populationGroups.reBuild(8_000, 500, 1000);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!calibrationEnabled) {
            return;
        }
        int interval = (int) Math.floor(1.0 / dmcWeight);
        int iteration = event.getIteration();
        // in the first iteration, we plot the boxes
        if (iteration==0) {
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, tripListConverter, iteration);
        }

        // then, each interval iterations, we update the ASCs and plot the boxes again
        if (iteration >= WARMUP_ITERATIONS && iteration % interval == 0) {
            rebuildPopulationGroupsIfRequired();
            updateASCs(iteration);
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, tripListConverter, iteration);
            numUpdates++;
        }
    }
}
