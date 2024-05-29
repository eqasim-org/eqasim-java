package org.eqasim.core.simulation.modes.drt.mode_choice.constraints;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.*;
import java.util.stream.Collectors;

public class DrtServiceTimeConstraint extends AbstractTripConstraint {

    private Map<String, Map<String, List<List<Integer>>>> timeSlotsPerDrtModePerMainMode = new HashMap<>();

    public DrtServiceTimeConstraint(Map<String, Map<String, List<List<Integer>>>> timeSlotsPerDrtModePerMainMode) {
        this.timeSlotsPerDrtModePerMainMode = timeSlotsPerDrtModePerMainMode;
    }

    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate tripCandidate, List<TripCandidate> previousCandidates) {
        if(this.timeSlotsPerDrtModePerMainMode.containsKey(tripCandidate.getMode()) && tripCandidate instanceof RoutedTripCandidate routedTripCandidate) {
            Map<String, List<List<Integer>>> timeSlotsPerDrtMode = timeSlotsPerDrtModePerMainMode.get(tripCandidate.getMode());
            for(PlanElement planElement: routedTripCandidate.getRoutedPlanElements()) {
                if(planElement instanceof Leg leg && timeSlotsPerDrtMode.containsKey(leg.getMode())) {
                    Double departureTime = leg.getDepartureTime().seconds();
                    List<List<Integer>> timeSlots = timeSlotsPerDrtMode.get(leg.getMode());
                    if(timeSlots.stream().noneMatch(slot -> departureTime >= slot.get(0) && departureTime <= slot.get(1))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static class Factory implements TripConstraintFactory {

        private final Map<String, Map<String, List<List<Integer>>>> timeSlotsPerDrtModePerMainMode;

        public Factory(Map<String, Map<String, List<List<Integer>>>> timeSlotsPerDrtModePerMainMode) {
            this.timeSlotsPerDrtModePerMainMode = timeSlotsPerDrtModePerMainMode;


            for(List<Integer> slot: this.timeSlotsPerDrtModePerMainMode.values().stream()
                    .flatMap(stringListMap -> stringListMap.values().stream())
                    .flatMap(Collection::stream)
                    .toList()) {
                if(slot.size() != 2) {
                    throw new IllegalStateException(String.format("Time slot should be defined with two values, %s given", slot.stream().map(String::valueOf).collect(Collectors.joining(", "))));
                }
                if(slot.get(0) >= slot.get(1)) {
                    throw new IllegalStateException(String.format("Time slot lower bound should be strictly less than the upper bound, %d and %d given", slot.get(0), slot.get(1)));
                }
            }
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> list, Collection<String> collection) {
            return new DrtServiceTimeConstraint(this.timeSlotsPerDrtModePerMainMode);
        }
    }
}
