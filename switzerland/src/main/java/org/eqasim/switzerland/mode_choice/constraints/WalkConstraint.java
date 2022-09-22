package org.eqasim.switzerland.mode_choice.constraints;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.constraints.AbstractTripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraint;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripConstraintFactory;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.Collection;
import java.util.List;


public class WalkConstraint extends AbstractTripConstraint {
    public static final String WALK_MODE = "walk";
    private Config config;

    public WalkConstraint(Config config) {
        this.config = config;
    }

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        if (mode.equals(WALK_MODE)) {

            double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
                    trip.getDestinationActivity().getCoord());
//            double walkSpeed = this.config.plansCalcRoute().getTeleportedModeSpeeds().get(WALK_MODE);
            double walkFactor = this.config.plansCalcRoute().getBeelineDistanceFactors().get(WALK_MODE);
            if (distance * walkFactor  > 5000)// 5km  is the limit for walking
                return false;
        }

        return true;
    }

    static public class Factory implements TripConstraintFactory {

        private Config config;

        public Factory(Config config) {
            this.config = config;
        }

        @Override
        public TripConstraint createConstraint(Person person, List<DiscreteModeChoiceTrip> planTrips,
                                               Collection<String> availableModes) {
            return new WalkConstraint(this.config);
        }
    }
}
