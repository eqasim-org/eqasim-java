package org.eqasim.core.components.traffic.bike;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class BikeTravelDisutilityFactory implements TravelDisutilityFactory {
    @Override
    public TravelDisutility createTravelDisutility(TravelTime travelTime) {
        return new BikeTravelDisutility(travelTime);
    }
}
