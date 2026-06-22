package org.eqasim.core.components.network_calibration.demand_calibration;

import com.google.inject.Provider;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.population.routes.NetworkRoute;

import java.util.List;

public class ODErrors {

    private final Population population;
    private final PopulationGroups populationGroups;
    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final TripListConverter tripListConverter;
    private final double sampleSize;
    private final boolean calibrationEnabled;

    private final double RELATIVE_DIFFERENCE_THRESHOLD;
    private final double EPSILON;
    private final double MAX_ABS_LOG_ERROR;
    private final double OBSERVATION_SHRINKAGE; // To build confidence in one OD erro
    private final double MIN_TRIP_WEIGHT;

    public ODErrors(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, Provider<CountsProcessor> countsProcessorProvider,
                    Provider<FlowProcessor> flowProcessorProvider, TripListConverter tripListConverter, EqasimConfigGroup eqasimConfig,
                    NetworkCalibrationConfigGroup calConfig) {
        this.population = scenario.getPopulation();
        this.tripListConverter = tripListConverter;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.calibrationEnabled = calConfig.getAllObjectives().contains("subpopulations") && calConfig.isCalibrationEnabled();

        this.countsProcessor = calibrationEnabled ? countsProcessorProvider.get():null;
        this.flowProcessor = calibrationEnabled ? flowProcessorProvider.get():null;
        this.populationGroups = calibrationEnabled ? populationGroupsProvider.get():null;

        // Later I need to make these parameters configurable
        this.RELATIVE_DIFFERENCE_THRESHOLD = 0.02; // threshold in relative flow error to start correcting
        this.EPSILON = 1.0; // for log error (log(x)+epsilon)
        this.MAX_ABS_LOG_ERROR = 1.5; //limit the log error with this range
        this.OBSERVATION_SHRINKAGE = 100.0 * sampleSize; // used for confidence weight
        this.MIN_TRIP_WEIGHT = 0.05; // minimum weight of each trips (the higher it crosses counting stations, the lower is the weight)
    }

    public double[][] getODCorrections() {
        return computeOdCorrections();
    }

    private double[][] computeOdCorrections() {
        int n = populationGroups.size();
        double[][] sumLogError = new double[n][n];
        int[][] observations = new int[n][n];
        double[][] sumWeights = new double[n][n];
        // We go through all the population, and we insert the errors into these matrices if that person passed through a counting station
        for (Person person : population.getPersons().values()) {
            if (!Tools.isInSubPopulation(person) && Tools.isCarAvailable(person)) {
                Plan plan = person.getSelectedPlan();
                insertErrors(sumLogError, observations, sumWeights, plan);
            }
        }
        // We compute the average error for each OD pair, and we return the matrix of corrections
        double[][] corrections = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                int nObs = observations[i][j];
                if (nObs == 0) {
                    continue;
                }

                double totalWeight = sumWeights[i][j];
                if (totalWeight < 1.0e-9) {
                    continue;
                }

                double meanLogError = sumLogError[i][j] / totalWeight;
                double confidenceWeight = nObs / (nObs + OBSERVATION_SHRINKAGE);
                corrections[i][j] = confidenceWeight * meanLogError;
            }
        }

        return corrections;
    }

    private void insertErrors(double[][] sumLogError, int[][] observations, double[][] sumWeights, Plan plan) {
        for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
            String mode = trip.getInitialMode();

            if (TransportMode.car.equals(mode)) {
                List<? extends PlanElement> elements = trip.getInitialElements();
                for (PlanElement element : elements) {
                    if (element instanceof Leg leg) {
                        insertErrors(sumLogError, observations, sumWeights, trip, leg);
                    }
                }
            }
        }
    }

    private void insertErrors(double[][] sumLogError, int[][] observations, double[][] sumWeights,
                              DiscreteModeChoiceTrip trip, Leg leg) {
        if (!(leg.getRoute() instanceof NetworkRoute)) {
            return;
        }

        NetworkRoute route = (NetworkRoute) leg.getRoute();
        List<Id<Link>> linkIds = route.getLinkIds();
        if (linkIds == null || linkIds.isEmpty()) {
            return;
        }

        // Count the number of counted links on this trip so we can weight the contribution.
        int countedLinksOnTrip = 0;
        for (Id<Link> linkId : linkIds) {
            if (countsProcessor.getLinkCounts(linkId) > 0) {
                countedLinksOnTrip++;
            }
        }

        // If the trip doesn't cross any counted link, it carries no information for this OD cell.
        if (countedLinksOnTrip == 0) {
            return;
        }

        // Each counted link on this trip contributes 1 / countedLinksOnTrip to the OD cell.
        // This way, a trip that crosses 3 counts adds +1/3 per link, for a total of +1.
        // A trip that crosses 20 counts adds +1/20 per link, for a total of +1.
        double linkWeight = 1.0 / countedLinksOnTrip;
        // As an extra safeguard, if a single trip has a very large number of counts, each individual link contribution is capped to avoid noise.
        linkWeight = Math.max(linkWeight, MIN_TRIP_WEIGHT);

        // we get the origin and destination zones
        Coord origin = trip.getOriginActivity().getCoord();
        Coord destination = trip.getDestinationActivity().getCoord();
        int groupOrigin = populationGroups.getGroup(origin);
        int groupDestination = populationGroups.getGroup(destination);

        for (Id<Link> linkId : linkIds) {
            float counts = countsProcessor.getLinkCounts(linkId);
            if (counts > 0) {
                double totalFlow = flowProcessor.getTotalLinkFlow(linkId);
                if (totalFlow > 0.0) {
                    totalFlow = totalFlow / sampleSize;
                    insertError(sumLogError, observations, sumWeights, groupOrigin, groupDestination,
                            counts, totalFlow, linkWeight);
                }
            }
        }
    }

    private void insertError(double[][] sumLogError, int[][] observations, double[][] sumWeights,
                             int groupOrigin, int groupDestination,
                             double counts, double totalFlow, double weight) {
        double pceDiff = (totalFlow - counts) / Math.max(counts, EPSILON);
        if (Math.abs(pceDiff) <= RELATIVE_DIFFERENCE_THRESHOLD) {
            return;
        }

        double logError = Math.log((counts + EPSILON) / (totalFlow + EPSILON));
        logError = Math.max(-MAX_ABS_LOG_ERROR, Math.min(MAX_ABS_LOG_ERROR, logError));

        sumLogError[groupOrigin][groupDestination] += logError * weight;
        sumWeights[groupOrigin][groupDestination] += weight;
        observations[groupOrigin][groupDestination] += 1;
    }
}
