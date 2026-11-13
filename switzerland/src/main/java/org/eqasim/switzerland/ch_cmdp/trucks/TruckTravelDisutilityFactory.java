package org.eqasim.switzerland.ch_cmdp.trucks;

import com.google.inject.Inject;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.ch_cmdp.tolls.MarginalCostOfTolls;
import org.eqasim.switzerland.ch_cmdp.tolls.Tolls;
import org.matsim.core.config.Config;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/** Creates truck costs directly, without consulting the car cost factory. */
public final class TruckTravelDisutilityFactory implements TravelDisutilityFactory {
    private final double distanceWeight;
    private final double sigma;
    private final long seed;
    private final Tolls tolls;
    private final MarginalCostOfTolls marginalCostOfTolls;

    @Inject public TruckTravelDisutilityFactory(EqasimConfigGroup settings, Config config,
            Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
        this(settings.getRoutingDistanceUtility(), settings.getSigmaRoutingRandomness(),
                config.global().getRandomSeed(), tolls, marginalCostOfTolls);
    }

    public TruckTravelDisutilityFactory(double distanceWeight, double sigma, long seed,
            Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
        this.distanceWeight = distanceWeight;
        this.sigma = sigma;
        this.seed = seed;
        this.tolls = tolls;
        this.marginalCostOfTolls = marginalCostOfTolls;
    }

    @Override public TravelDisutility createTravelDisutility(TravelTime travelTime) {
        return new TruckTravelDisutility(travelTime, distanceWeight, sigma, seed, tolls, marginalCostOfTolls);
    }
}
