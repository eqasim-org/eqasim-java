package org.eqasim.switzerland.ch_cmdp.trucks;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/** Delegates to the live car travel-time instance, without allocating another time table. */
public final class TruckTravelTime implements TravelTime {
    private final TravelTime carTravelTime;

    @Inject public TruckTravelTime(@Named(TransportMode.car) TravelTime carTravelTime) {
        this.carTravelTime = carTravelTime;
    }

    @Override public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
        return carTravelTime.getLinkTravelTime(link, time, person, vehicle);
    }
}
