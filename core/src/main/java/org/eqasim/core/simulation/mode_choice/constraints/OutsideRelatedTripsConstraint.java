package org.eqasim.core.simulation.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

import java.util.Collection;
import java.util.List;

public class OutsideRelatedTripsConstraint extends AbstractTripConstraint {

    public static final String OUTSIDE_ACTIVITY_TYPE = "outside";

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        if(trip.getOriginActivity().getType().equals(OUTSIDE_ACTIVITY_TYPE) || trip.getDestinationActivity().getType().equals(OUTSIDE_ACTIVITY_TYPE)) {
            return trip.getInitialMode().equals(mode);
        }
        return true;
    }

    static public class Factory implements TripConstraintFactory {

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips, Collection<String> availableModes) {
            return new OutsideRelatedTripsConstraint();
        }
    }
}
