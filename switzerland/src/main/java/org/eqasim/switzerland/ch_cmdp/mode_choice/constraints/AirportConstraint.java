package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

/** Keeps the current mode of trips to or from an airport, without restricting rerouting. */
public class AirportConstraint extends AbstractTripConstraint {
    public static final String AIRPORT_ACTIVITY = "airport";

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        boolean touchesAirport = AIRPORT_ACTIVITY.equals(trip.getOriginActivity().getType())
                || AIRPORT_ACTIVITY.equals(trip.getDestinationActivity().getType());
        return !touchesAirport || trip.getInitialMode().equals(mode);
    }

    public static class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                Collection<String> availableModes) {
            return new AirportConstraint();
        }
    }
}
