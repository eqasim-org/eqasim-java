package org.eqasim.core.components.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic_light.TrafficLightConfigGroup;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficModule extends AbstractModule {
    private static final Logger logger = LogManager.getLogger(EqasimTrafficModule.class);
    @Override
    public void install() {
        TrafficLightConfigGroup tlConfig = TrafficLightConfigGroup.getOrCreate(getConfig());
        if (tlConfig.isActivated()) {
            logger.info("Traffic light crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(TrafficLightCrossingPenalty.class);
        } else {
            logger.info("Attribute crossing penalty is enabled.");
            bind(CrossingPenalty.class).to(AttributeCrossingPenalty.class);
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
    TrafficLightCrossingPenalty provideTrafficLightCrossingPenalty(Network network, AttributeCrossingPenalty delegate,
                                                                    TrafficLightDelay tlDelays) {
        return TrafficLightCrossingPenalty.build(network, delegate, tlDelays);
    }
}
