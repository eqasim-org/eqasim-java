package org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs;

import com.google.inject.Provider;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
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

    private final double relativeDifferenceThreshold;
    private final double epsilon;
    private final double maxAbsoluteLogError;
    private final double observationShrinkage;
    private final double minTripWeight;
    private final double lowCountWeight;
    private final double mediumCountWeight;
    private final double highCountWeight;
    private final double veryHighCountWeight;

    private final double counts25Percentile;
    private final double counts50Percentile;
    private final double counts75Percentile;

    public ODErrors(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, Provider<CountsProcessor> countsProcessorProvider,
                    Provider<FlowProcessor> flowProcessorProvider, TripListConverter tripListConverter, EqasimConfigGroup eqasimConfig,
                    NetworkCalibrationConfigGroup calConfig, AgentAscsCalibrationConfigGroup agentConfig) {
        this.population = scenario.getPopulation();
        this.tripListConverter = tripListConverter;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.calibrationEnabled = calConfig.isActivated() && calConfig.isCalibrationEnabled()
                && calConfig.isAgentAscsCalibrationActivated();

        this.countsProcessor = calibrationEnabled ? countsProcessorProvider.get():null;
        this.flowProcessor = calibrationEnabled ? flowProcessorProvider.get():null;
        this.populationGroups = calibrationEnabled ? populationGroupsProvider.get():null;

        this.counts25Percentile = countsProcessor==null? 0.0:countsProcessor.getPercentile(25);
        this.counts50Percentile = countsProcessor==null? 0.0:countsProcessor.getPercentile(50);
        this.counts75Percentile = countsProcessor==null? 0.0:countsProcessor.getPercentile(75);

        this.relativeDifferenceThreshold = agentConfig.getRelativeFlowErrorThreshold();
        this.epsilon = agentConfig.getLogErrorEpsilon();
        this.maxAbsoluteLogError = agentConfig.getMaxAbsoluteLogError();
        this.observationShrinkage = agentConfig.getObservationShrinkage() * sampleSize;
        this.minTripWeight = agentConfig.getMinTripWeight();
        this.lowCountWeight = agentConfig.getLowCountWeight();
        this.mediumCountWeight = agentConfig.getMediumCountWeight();
        this.highCountWeight = agentConfig.getHighCountWeight();
        this.veryHighCountWeight = agentConfig.getVeryHighCountWeight();
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
                double confidenceWeight = nObs / (nObs + observationShrinkage);
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
            if (countsProcessor.contains(linkId)) {
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
        linkWeight = Math.max(linkWeight, minTripWeight);

        // we get the origin and destination zones
        Coord origin = trip.getOriginActivity().getCoord();
        Coord destination = trip.getDestinationActivity().getCoord();
        int groupOrigin = populationGroups.getGroup(origin);
        int groupDestination = populationGroups.getGroup(destination);

        for (Id<Link> linkId : linkIds) {
            float counts = countsProcessor.getLinkCounts(linkId);
            float countWeight = countsProcessor.getWeight(linkId);

            if (counts > 0) {
                double totalFlow = flowProcessor.getTotalLinkFlow(linkId);
                if (totalFlow > 0.0) {
                    totalFlow = totalFlow / sampleSize;
                    insertError(sumLogError, observations, sumWeights, groupOrigin, groupDestination,
                            counts, totalFlow, linkWeight, countWeight);
                }
            }
        }
    }

    private void insertError(double[][] sumLogError, int[][] observations, double[][] sumWeights,
                             int groupOrigin, int groupDestination,
                             double counts, double totalFlow, double linkWeight, double countWeight) {
        double pceDiff = (totalFlow - counts) / Math.max(counts, epsilon);
        if (Math.abs(pceDiff) <= relativeDifferenceThreshold) {
            return;
        }

        double logError = Math.log((counts + epsilon) / (totalFlow + epsilon));
        logError = Math.max(-maxAbsoluteLogError, Math.min(maxAbsoluteLogError, logError));
        // At this point, one should only use the weight, however, I think that we must respect more the flow in the highways
        // thus, I think that we need to include an additional weight that is based on the counts, it is higher as the counts go up
        double w = linkWeight * getCountsWeight(counts) * countWeight;

        sumLogError[groupOrigin][groupDestination] += logError * w;
        sumWeights[groupOrigin][groupDestination] += w;
        observations[groupOrigin][groupDestination] += 1;
    }

    private double getCountsWeight(double count){
        if(count <= counts25Percentile){
            return lowCountWeight;
        } else if(count <= counts50Percentile){
            return mediumCountWeight;
        } else if(count <= counts75Percentile){
            return highCountWeight;
        } else {
            return veryHighCountWeight;
        }
    }
}
