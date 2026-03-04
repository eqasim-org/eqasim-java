package org.eqasim.core.components.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.bike.BikeGradientBasedLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeTravelDisutilityFactory;
import org.eqasim.core.components.traffic.bike.BikeTravelTime;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.delays.IntersectionDelay;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficModule extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(EqasimTrafficModule.class);
    @Override
    public void install() {
        // here we check whether the traffic light crossing penalty is based on delays or attributes, and bind the appropriate implementation accordingly
        Config config = getConfig();
        DelaysConfigGroup tlConfig = DelaysConfigGroup.getOrCreate(config);
        if (tlConfig.isActivated()) {
            logger.info("Traffic light crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(IntersectionDelay.class);
        } else {
            logger.info("Attribute crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(AttributeCrossingPenalty.class);
        }

        // this is for bike travel times when it is routed in the network
        bind(BikeSpeedCalculator.class).to(BikeGradientBasedLinkSpeedCalculator.class);
        addTravelTimeBinding(TransportMode.bike).to(BikeTravelTime.class).in(Singleton.class);
        addTravelDisutilityFactoryBinding(TransportMode.bike).to(BikeTravelDisutilityFactory.class).in(Singleton.class);

        // if the vdf module is not activated, we need to bind the default link speed calculator, otherwise, it will be bound by the vdf module
        boolean vdfActivated = config.getModules().get(VDFConfigGroup.GROUP_NAME) != null;
        if (!vdfActivated) {
            bind(EqasimLinkSpeedCalculator.class).to(DefaultEqasimLinkSpeedCalculator.class);
        }
    }

    @Provides
    @Singleton
    AttributeCrossingPenalty providAttributeCrossingPenalty(Network network, DefaultCrossingPenalty delegate) {
        return AttributeCrossingPenalty.sbuild(network, delegate);
    }

    @Provides
    @Singleton
    DefaultCrossingPenalty provideDefaultCrossingPenalty(Network network, EqasimConfigGroup eqasimConfig) {
        return DefaultCrossingPenalty.build(network, eqasimConfig.getCrossingPenalty());
    }

    @Provides
    @Singleton
    BikeGradientBasedLinkSpeedCalculator provideBikeGradientBasedLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
        return new BikeGradientBasedLinkSpeedCalculator(crossingPenalty);
    }

    @Provides
    @Singleton
    DefaultEqasimLinkSpeedCalculator provideDefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty, BikeSpeedCalculator bikeSpeedCalculator) {
        return new DefaultEqasimLinkSpeedCalculator(crossingPenalty, bikeSpeedCalculator);
    }

    @Provides
    @Singleton
    BikeTravelDisutilityFactory provideBikeTravelDisutilityFactory() {
        return new BikeTravelDisutilityFactory();
    }


}
