package org.eqasim.core.simulation.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;

import java.util.Collection;
import java.util.List;

public class ActivityEndTimeConstraint extends AbstractTripConstraint {

    @Override
    public boolean validateAfterEstimation(DiscreteModeChoiceTrip trip, TripCandidate candidate, List<TripCandidate> previousCandidates) {
        // We consider that activities should always have well defined start time and time
        // Thus we allow exceptions to be thrown below if a time is missing
        double destinationActivityEndTime = trip.getDestinationActivity().getEndTime().seconds();
        double originActivityEndTime = trip.getOriginActivity().getEndTime().seconds();
        // We do not consider the possible delays from previous trips here as the application of the constraint on them will ensure that not delay is generated
        return originActivityEndTime + candidate.getDuration() <= destinationActivityEndTime;
    }


    static public class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new ActivityEndTimeConstraint();
        }
    }
}