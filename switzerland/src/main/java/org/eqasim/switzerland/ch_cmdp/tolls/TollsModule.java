package org.eqasim.switzerland.ch_cmdp.tolls;

import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TollsModule extends AbstractModule {
    @Override
    public void install() {
        addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTollsTravelDisutilityFactory.class);
        addTravelDisutilityFactoryBinding(TransportMode.truck).to(EqasimTollsTravelDisutilityFactory.class);
    }

    @Provides
    @Singleton
    EqasimTollsTravelDisutilityFactory provideEqasimTollsTravelDisutilityFactory(EqasimTravelDisutilityFactory delegate, Tolls tolls, MarginalCostOfTolls marginalCostOfTolls) {
    	return new EqasimTollsTravelDisutilityFactory(delegate,  tolls, marginalCostOfTolls);
    }

    @Provides
    @Singleton
    Tolls provideTolls(Network network) {
        return new Tolls(network);
    }

    @Provides
    @Singleton
    MarginalCostOfTolls provideMarginalCostOfTolls() {
        Config config = getConfig();
        NetworkCalibrationConfigGroup netConfig = NetworkCalibrationConfigGroup.getOrCreate(config);

        double sigma = config.routing().getRoutingRandomness();
        double valueOfTime = netConfig.getTollsValueOfTime();
        long baseSeed = config.global().getRandomSeed();
        return new MarginalCostOfTolls(sigma, valueOfTime, baseSeed);
    }

}
