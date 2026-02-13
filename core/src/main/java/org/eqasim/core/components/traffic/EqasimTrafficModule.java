package org.eqasim.core.components.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.bike.BikeGradientBasedLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeTravelDisutilityFactory;
import org.eqasim.core.components.traffic.bike.BikeTravelTime;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.delays.IntersectionDelay;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficModule extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(EqasimTrafficModule.class);
    @Override
    public void install() {
        // here we check whether the traffic light crossing penalty is based on delays or attributes, and bind the appropriate implementation accordingly
        DelaysConfigGroup tlConfig = DelaysConfigGroup.getOrCreate(getConfig());
        if (tlConfig.isActivated()) {
            logger.info("Traffic light crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(IntersectionDelay.class);
        } else {
            logger.info("Attribute crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(AttributeCrossingPenalty.class);
        }

        bind(EqasimLinkSpeedCalculator.class).to(DefaultEqasimLinkSpeedCalculator.class);

        // here we check whether the bike is routed in the network or not
        boolean bikeIsRouted = getConfig().routing().getNetworkModes().contains(TransportMode.bike);
        if (bikeIsRouted) {
            logger.info("Bike mode detected in routing configuration. Using bike-specific implementations for link speed and travel disutility.");
            bind(BikeLinkSpeedCalculator.class).to(BikeGradientBasedLinkSpeedCalculator.class);
            addTravelTimeBinding(TransportMode.bike).to(BikeTravelTime.class).in(Singleton.class);
            addTravelDisutilityFactoryBinding(TransportMode.bike).to(BikeTravelDisutilityFactory.class).in(Singleton.class);
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
    BikeGradientBasedLinkSpeedCalculator provideBikeGradientBasedLinkSpeedCalculator(DefaultEqasimLinkSpeedCalculator carSpeedCalculator) {
        return new BikeGradientBasedLinkSpeedCalculator(carSpeedCalculator);
    }
    @Provides
    @Singleton
    DefaultEqasimLinkSpeedCalculator provideDefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
        return new DefaultEqasimLinkSpeedCalculator(crossingPenalty);
    }

    @Provides
    @Singleton
    BikeTravelDisutilityFactory provideBikeTravelDisutilityFactory() {
        return new BikeTravelDisutilityFactory();
    }


}
