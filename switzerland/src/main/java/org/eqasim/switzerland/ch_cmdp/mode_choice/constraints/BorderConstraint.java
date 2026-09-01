package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;

import java.util.Collection;
import java.util.List;


public class BorderConstraint extends AbstractTripConstraint {
    public static final String BORDER_ACTIVITY = "border";

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {

        boolean destinationBorderActivity = trip.getDestinationActivity().getType().equals(BORDER_ACTIVITY);
        boolean originBorderActivity = trip.getOriginActivity().getType().equals(BORDER_ACTIVITY);

        if (!destinationBorderActivity && !originBorderActivity) {
            return true;
        }
        return trip.getInitialMode().equals(mode);
    }

    static public class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new BorderConstraint();
        }
    }
}
