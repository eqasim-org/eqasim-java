package org.eqasim.core.components.traffic.bike;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;
import com.google.inject.Inject;

public class BikeTravelTime implements TravelTime {

    private final BikeLinkSpeedCalculator linkSpeedCalculator;

    @Inject
    public BikeTravelTime(BikeLinkSpeedCalculator linkSpeedCalculator) {
        this.linkSpeedCalculator = linkSpeedCalculator;
    }

    @Override
    public double getLinkTravelTime(Link link, double v, Person person, Vehicle vehicle) {
        return link.getLength()/linkSpeedCalculator.maximumBikeSpeed(vehicle, link, v);
    }
}
