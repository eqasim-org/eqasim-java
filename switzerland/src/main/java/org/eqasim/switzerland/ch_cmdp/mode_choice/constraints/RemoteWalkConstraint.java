package org.eqasim.switzerland.ch_cmdp.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import java.util.Collection;
import java.util.List;
import static org.eqasim.switzerland.ch_cmdp.mode_choice.constraints.LoopModesConstraint.modeConstraint;

public class RemoteWalkConstraint extends AbstractTripConstraint {
    public static final String REMOTE_MODE = "remote_walk";

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        return modeConstraint(trip, mode, REMOTE_MODE);
    }

    static public class Factory implements TripConstraintFactory {
        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new RemoteWalkConstraint();
        }
    }
}
