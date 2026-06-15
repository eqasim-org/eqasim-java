package org.eqasim.core.components.network_calibration.demand_calibration;

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

public class ASCsAdapter implements IterationEndsListener {

    // --- Softened hyperparameters ---
    private static final double LEARNING_RATE = 0.5;
    private static final double MAX_PERSON_ASC_STEP = 0.2;
    private static final double MAX_PERSON_ASC = 2.0;
    private static final int WARMUP_ITERATIONS = 20;
    private static final double LEARNING_RATE_DECAY = 0.95;

    private final OutputDirectoryHierarchy outputHierarchy;
    private final Population population;
    private final PopulationGroups populationGroups;
    private final TripListConverter tripListConverter;
    private final ODErrors odErrors;
    private final double dmcWeight;
    private final boolean activate;
    private final double initialLearningRate;

    public ASCsAdapter(Scenario scenario, PopulationGroups populationGroups, TripListConverter tripListConverter,
                       OutputDirectoryHierarchy outputHierarchy, ODErrors odErrors, double dmcWeight, boolean activate) {
        this.population = scenario.getPopulation();
        this.populationGroups = populationGroups;
        this.tripListConverter = tripListConverter;
        this.odErrors = odErrors;
        this.dmcWeight = dmcWeight;
        this.activate = activate;
        this.outputHierarchy = outputHierarchy;
        this.initialLearningRate = LEARNING_RATE;

        for (Person person : scenario.getPopulation().getPersons().values()) {
            Tools.setCarASC(person, 0.0);
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
                avgDelta = Math.max(-MAX_PERSON_ASC_STEP, Math.min(MAX_PERSON_ASC_STEP, avgDelta));
                Tools.incrementCarASC(person, avgDelta, MAX_PERSON_ASC);
            }
        }
    }

    private double getDeltaAsc(double odCorrection, double currentLearningRate) {
        return currentLearningRate * odCorrection;
    }

    private double currentLearningRate(int iteration) {
        int effectiveIteration = Math.max(0, iteration - WARMUP_ITERATIONS);
        return initialLearningRate * Math.pow(LEARNING_RATE_DECAY, effectiveIteration);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!activate) {
            return;
        }
        int interval = (int) Math.floor(1.0 / dmcWeight);
        int iteration = event.getIteration();
        // in the first iteration, we plot the boxes
        if (iteration==0) {
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, iteration);
        }
        // then, each interval iterations, we update the ASCs and plot the boxes again
        if (iteration >= WARMUP_ITERATIONS && iteration % interval == 0) {
            updateASCs(iteration);
            correctionHeatMap.plotAverageCarAsc(population, populationGroups, outputHierarchy, iteration);
        }
    }
}
