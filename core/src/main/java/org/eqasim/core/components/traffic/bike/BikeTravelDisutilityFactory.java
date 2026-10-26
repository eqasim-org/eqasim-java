package org.eqasim.core.components.traffic.bike;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class BikeTravelDisutilityFactory implements TravelDisutilityFactory {
    private final BikeLinkData linkData;

    public BikeTravelDisutilityFactory() { this(null); }

    public BikeTravelDisutilityFactory(BikeLinkData linkData) {
        this.linkData = linkData;
    }
    @Override
    public TravelDisutility createTravelDisutility(TravelTime travelTime) {
        return new BikeTravelDisutility(travelTime, linkData);
    }
}
