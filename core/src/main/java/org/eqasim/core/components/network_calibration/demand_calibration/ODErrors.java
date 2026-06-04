package org.eqasim.core.components.network_calibration.demand_calibration;

import org.eqasim.core.components.flow.LinkFlowCounter;
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
    private final double PERCENTAGE_DIFFERENCE_THRESHOLD = 0.05;

    public ODErrors(Scenario scenario, PopulationGroups populationGroups, CountsProcessor countsProcessor,
                    FlowProcessor flowProcessor, TripListConverter tripListConverter, double sampleSize) {
        this.population = scenario.getPopulation();
        this.populationGroups = populationGroups;
        this.countsProcessor = countsProcessor;
        this.flowProcessor = flowProcessor;
        this.tripListConverter = tripListConverter;
        this.sampleSize = sampleSize;
    }

    public int[][] getODErrors() {
        return computeOdErrors();
    }

    private int[][] computeOdErrors(){
        int n = populationGroups.size();
        int[][] odErrors = new int[n][n];

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            insertError(odErrors, plan);
        }
        return odErrors;
    }

    private void insertError(int[][] odErrors, Plan plan) {
        for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
            String mode = trip.getInitialMode();

            if (TransportMode.car.equals(mode)) {
                List<? extends PlanElement> elements = trip.getInitialElements();
                for (PlanElement element : elements) {
                    if (element instanceof Leg leg){
                        insertError(odErrors, trip, leg);
                    }
                }
            }
        }
    }

    private void insertError(int[][] odErrors, DiscreteModeChoiceTrip trip, Leg leg) {
        NetworkRoute route = (NetworkRoute) leg.getRoute();
        List<Id<Link>> linkIds = route.getLinkIds();

        for (Id<Link> linkId : linkIds) {
            float counts = countsProcessor.getLinkCounts(linkId); // if lower than 0, counts do not exist for this link
            if (counts > 0){
                double totalFlow = flowProcessor.getTotalLinkFlow(linkId);
                if (totalFlow > 1){
                    totalFlow = totalFlow/sampleSize;
                    insertError(odErrors, trip, counts, totalFlow);
                }
            }
        }
    }

    private void insertError(int[][] odErrors, DiscreteModeChoiceTrip trip, double counts, double totalFlow){
        double pceDiff = (totalFlow - counts)/counts;
        if (pceDiff>PERCENTAGE_DIFFERENCE_THRESHOLD){
            Coord origin = trip.getOriginActivity().getCoord();
            Coord destination = trip.getDestinationActivity().getCoord();
            int groupOrigin = populationGroups.getGroup(origin);
            int groupDestination = populationGroups.getGroup(destination);
            odErrors[groupOrigin][groupDestination] += 1;
        }
    }
}
