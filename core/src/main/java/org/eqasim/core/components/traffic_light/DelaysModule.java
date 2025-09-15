package org.eqasim.core.components.traffic_light;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.DefaultCrossingPenalty;
import org.eqasim.core.components.traffic_light.delays.IntersectionDelay;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.delays.UnsignalizedIntersectionDelay;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparConfigGroup;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparDelay;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterDelay;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterFormula;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class DelaysModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(DelaysModule.class);

    @Override
    protected void installEqasimExtension() {
        DelaysConfigGroup delaysConfig = DelaysConfigGroup.getOrCreate(getConfig());
        if (delaysConfig.isActivated()) {
            logger.info("Traffic light module is enabled.");
            addEventHandlerBinding().to(TrafficCounter.class).asEagerSingleton();
            addControlerListenerBinding().to(DelaysUpdaterListener.class).asEagerSingleton();
        } else {
            logger.info("Traffic light is disabled, skipping installation.");
        }
    }

    @Provides
    @Singleton
    public DelaysUpdaterListener provideTrafficLightListener(DelaysConfigGroup delaysConfigGroup, FlowDataSet flowDataSet, TrafficCounter trafficCounter,
                                                             TrafficLightDelay trafficLightDelay, UnsignalizedIntersectionDelay unsignalizedIntersectionDelay,
                                                             OutputDirectoryHierarchy outputHierarchy, IntersectionDelay intersectionDelay) {
        return new DelaysUpdaterListener(
                delaysConfigGroup, flowDataSet, trafficCounter, trafficLightDelay, unsignalizedIntersectionDelay, outputHierarchy, intersectionDelay);
    }

    @Provides
    @Singleton
    public TimeBinManager provideTimeBinManager(DelaysConfigGroup config) {
        return new TimeBinManager(config);
    }

    @Provides
    @Singleton
    public FlowDataSet provideFlowDataSet(Network network, TimeBinManager timeBinManager, DelaysConfigGroup config) {
        return new FlowDataSet(network, timeBinManager, config.getBeta());
    }

    @Provides
    @Singleton
    public TrafficCounter provideTrafficCounter(Network network, TimeBinManager timeBinManager) {
        return new TrafficCounter(network, timeBinManager);
    }



    @Provides
    @Singleton
    public IntersectionDelay provudeIntersectionDelay(DelaysConfigGroup delaysConfigGroup,
                                                      TrafficLightDelay trafficLightDelays,
                                                      UnsignalizedIntersectionDelay unsignalizedIntersectionDelay,
                                                      TimeBinManager timeBinManager,
                                                      DefaultCrossingPenalty delegate) {
        return new IntersectionDelay(delaysConfigGroup, trafficLightDelays, unsignalizedIntersectionDelay,
                                     timeBinManager, delegate);
    }

    @Provides
    @Singleton
    public TrafficLightDelay provideTrafficLightDelay(Network network, TimeBinManager timeBinManager, WebsterDelay websterDelay,
                                                      DelaysConfigGroup delaysConfigGroup) {
        return new TrafficLightDelay(network, timeBinManager, delaysConfigGroup, websterDelay);

    }

    @Provides
    @Singleton
    public UnsignalizedIntersectionDelay provideUnsignalizedIntersectionDelay(ShahparDelay shahparDelay) {
        return new UnsignalizedIntersectionDelay(shahparDelay);
    }



    @Provides
    @Singleton
    public ShahparDelay provideShahparDelay(Network network, FlowDataSet flowDataSet, TimeBinManager timeBinManager, DelaysConfigGroup delaysConfigGroup,
                                            EqasimConfigGroup eqasimConfigGroup) {
        return new ShahparDelay(network, flowDataSet, timeBinManager, delaysConfigGroup.getShahparConfigGroup(),
                                eqasimConfigGroup.getSampleSize());
    }


    @Provides
    @Singleton
    public WebsterDelay provideWebsterDelay(Network network, TimeBinManager timeBinManager,
                                            WebsterFormula webster, FlowDataSet flow, EqasimConfigGroup eqasimConfigGroup) {
        return new WebsterDelay(network, timeBinManager, webster, flow, eqasimConfigGroup.getSampleSize());
    }


    @Provides
    @Singleton
    public WebsterFormula provideWebsterFormula(DelaysConfigGroup delaysConfigGroup) {
        return new WebsterFormula(delaysConfigGroup.getWebsterConfigGroup());
    }

}
