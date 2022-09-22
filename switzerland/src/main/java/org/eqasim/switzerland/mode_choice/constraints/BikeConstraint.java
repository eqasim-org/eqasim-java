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


public class BikeConstraint extends AbstractTripConstraint {
    public static final String BIKE_MODE = "bike";
    private Config config;

    public BikeConstraint(Config config) {
        this.config = config;
    }

    @Override
    public boolean validateBeforeEstimation(DiscreteModeChoiceTrip trip, String mode, List<String> previousModes) {
        if (mode.equals(BIKE_MODE)) {

            double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(),
                    trip.getDestinationActivity().getCoord());
            if (distance  > 20000)// 20km  is the limit for biking
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
            return new BikeConstraint(this.config);
        }
    }
}
