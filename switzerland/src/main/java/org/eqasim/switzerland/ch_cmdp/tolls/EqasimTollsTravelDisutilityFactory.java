package org.eqasim.switzerland.ch_cmdp.tolls;

import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class EqasimTollsTravelDisutilityFactory implements TravelDisutilityFactory {
    private final EqasimTravelDisutilityFactory delegate;
    private final Tolls tolls;
    private final MarginalCostOfTolls marginalCostOfTolls;

    public EqasimTollsTravelDisutilityFactory(EqasimTravelDisutilityFactory delegate, Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
        this.delegate = delegate;
        this.tolls = tolls;
        this.marginalCostOfTolls = marginalCostOfTolls;
    }

    @Override
    public TravelDisutility createTravelDisutility(TravelTime travelTime) {
        return new EqasimTollsTravelDisutility(delegate.createTravelDisutility(travelTime), tolls, marginalCostOfTolls);
    }
}
