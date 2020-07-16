package org.eqasim.san_francisco.bike.routing;

import com.google.inject.Inject;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;


public class SanFranciscoBikeTravelTime implements TravelTime {

    @Inject
    private SanFranciscoBikeLinkSpeedCalculator sanFranciscoBikeLinkSpeedCalculator;

    @Inject
    private SanFranciscoBikeTravelTime() {}

    @Override
    public double getLinkTravelTime(Link link, double v, Person person, Vehicle vehicle) {
        return link.getLength() / sanFranciscoBikeLinkSpeedCalculator.getMaximumVelocityForLink(link, vehicle);
    }
}
