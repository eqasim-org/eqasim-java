package org.eqasim.core.components.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/** Optional routing fast path. Reuse a link time only when its calculator is identical. */
public interface TravelTimeAwareDisutility extends TravelDisutility {
    TravelTime getTravelTimeCalculator();

    double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle, double travelTime);
}
