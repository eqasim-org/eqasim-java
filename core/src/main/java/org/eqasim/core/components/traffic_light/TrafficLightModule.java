package org.eqasim.core.components.traffic_light;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.delays.UnsignalizedIntersectionDelay;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparDelay;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterFormula;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TrafficLightModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(TrafficLightModule.class);

    @Override
    protected void installEqasimExtension() {
        TrafficLightConfigGroup tlConfig = TrafficLightConfigGroup.getOrCreate(getConfig());
        if (tlConfig.isActivated()) {
            logger.info("Traffic light module is enabled.");
            addEventHandlerBinding().to(TrafficCounter.class).asEagerSingleton();
            addControlerListenerBinding().to(TrafficLightListener.class).asEagerSingleton();
        } else {
            logger.info("Traffic light is disabled, skipping installation.");
        }
    }

    @Provides
    @Singleton
    public TimeBinManager provideTimeBinManager(TrafficLightConfigGroup config) {
        return new TimeBinManager(config.getStartTime(), config.getEndTime(), config.getBinSize());
    }

    @Provides
    @Singleton
    public FlowDataSet provideFlowDataSet(Network network, TimeBinManager timeBinManager, TrafficLightConfigGroup config) {
        return new FlowDataSet(network, timeBinManager, config.getBeta());
    }

    @Provides
    @Singleton
    public TrafficCounter provideTrafficCounter(Network network, TimeBinManager timeBinManager) {
        return new TrafficCounter(network, timeBinManager);
    }


    @Provides
    @Singleton
    public WebsterFormula provideWebsterFormula(TrafficLightConfigGroup trafficLightConfigGroup) {
        return new WebsterFormula(trafficLightConfigGroup.getWebsterConfigGroup());
    }

    @Provides
    @Singleton
    public TrafficLightDelay provideTrafficLightDelay(Network network,
            TimeBinManager timeBinManager, WebsterFormula webster, TrafficLightConfigGroup trafficLightConfigGroup,
            EqasimConfigGroup eqasimConfig) {
        return new TrafficLightDelay(network, timeBinManager, webster, trafficLightConfigGroup, eqasimConfig.getSampleSize());

    }


    @Provides
    @Singleton
    public ShahparDelay provideShahparDelay(Network network, FlowDataSet flowDataSet, EqasimConfigGroup eqasimConfig) {
        return new ShahparDelay(network, flowDataSet, eqasimConfig.getSampleSize());
    }

    @Provides
    @Singleton
    public UnsignalizedIntersectionDelay provideUnsignalizedIntersectionDelay(FlowDataSet flowDataSet, Network network, ShahparDelay shahparDelay) {
        return new UnsignalizedIntersectionDelay(flowDataSet, network, shahparDelay);
    }

    @Provides
    @Singleton
    public TrafficLightListener provideTrafficLightListener(TrafficLightConfigGroup trafficLightConfigGroup, FlowDataSet flowDataSet, TrafficCounter trafficCounter,
                                                            TrafficLightDelay trafficLightDelay, OutputDirectoryHierarchy outputHierarchy) {
        return new TrafficLightListener(
                trafficLightConfigGroup, flowDataSet, trafficCounter, trafficLightDelay, outputHierarchy);
    }

}
