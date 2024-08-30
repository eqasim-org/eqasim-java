package org.eqasim.core.simulation.mode_choice.constraints.leg_time;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class LegTimeConstraint extends AbstractTripConstraint {
    Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> singleLegParameterSetByMainModeByLegMode;

    public LegTimeConstraint(Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> singleLegParameterSetByMainModeByLegMode) {
        this.singleLegParameterSetByMainModeByLegMode = singleLegParameterSetByMainModeByLegMode;
    }

    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate tripCandidate, List<TripCandidate> previousCandidates) {
        if(this.singleLegParameterSetByMainModeByLegMode.containsKey(tripCandidate.getMode()) && tripCandidate instanceof RoutedTripCandidate routedTripCandidate) {
            Map<String, LegTimeConstraintSingleLegConfigGroup> timeSlotsPerLegMode = singleLegParameterSetByMainModeByLegMode.get(tripCandidate.getMode());
            for(PlanElement planElement: routedTripCandidate.getRoutedPlanElements()) {
                if(planElement instanceof Leg leg && timeSlotsPerLegMode.containsKey(leg.getMode())) {
                    LegTimeConstraintSingleLegConfigGroup legTimeConstraintSingleLegConfigGroup = timeSlotsPerLegMode.get(leg.getMode());
                    double departureTime = leg.getDepartureTime().seconds();
                    double arrivalTime = departureTime + (legTimeConstraintSingleLegConfigGroup.checkBothDepartureAndArrivalTimes ? leg.getTravelTime().orElse(0.0): 0.0);
                    if(legTimeConstraintSingleLegConfigGroup.getTimeSlotsParameterSets().stream().noneMatch(slot -> departureTime >= slot.beginTime && arrivalTime <= slot.endTime)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static class Factory implements TripConstraintFactory {
        Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> singleLegParameterSetByMainModeByLegMode;

        public Factory(Map<String, Map<String, LegTimeConstraintSingleLegConfigGroup>> singleLegParameterSetByMainModeByLegMode) {
            this.singleLegParameterSetByMainModeByLegMode = singleLegParameterSetByMainModeByLegMode;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> list, Collection<String> collection) {
            return new LegTimeConstraint(this.singleLegParameterSetByMainModeByLegMode);
        }
    }
}
