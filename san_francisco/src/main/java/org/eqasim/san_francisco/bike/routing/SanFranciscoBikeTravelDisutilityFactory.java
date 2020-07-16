package org.eqasim.san_francisco.bike.routing;

import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class SanFranciscoBikeTravelDisutilityFactory implements TravelDisutilityFactory {

    @Override
    public TravelDisutility createTravelDisutility(TravelTime timeCalculator) {
        return new SanFranciscoBikeTravelDisutility();
    }
}
