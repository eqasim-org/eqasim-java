package org.eqasim.core.components.flow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class FlowModule extends AbstractEqasimExtension {

    private static final Logger LOGGER = LogManager.getLogger(FlowModule.class);

    @Override
    protected void installEqasimExtension() {
        FlowConfigGroup flowConfig = FlowConfigGroup.getOrCreate(getConfig());
        if (flowConfig.isComputerFlow()) {
            LOGGER.info("Flow module is enabled.");
            addControlerListenerBinding().to(FlowUpdaterListner.class);
            addEventHandlerBinding().to(TrafficCounter.class);
        } else {
            LOGGER.info("Flow module is disabled, skipping installation.");
        }

    }

    @Provides
    @Singleton
    public TimeBinManager provideTimeBinManager(FlowConfigGroup config) {
        return new TimeBinManager(config.getStartTime(), config.getEndTime(), config.getBinSize());
    }

    @Provides
    @Singleton
    public FlowDataSet provideFlowDataSet(Network network, TimeBinManager timeBinManager, FlowConfigGroup config) {
        return new FlowDataSet(network, timeBinManager, config.getBeta());
    }

    @Provides
    @Singleton
    public TrafficCounter provideTrafficCounter(Network network, TimeBinManager timeBinManager) {
        return new TrafficCounter(network, timeBinManager);
    }

    @Provides
    @Singleton
    public FlowUpdaterListner provideFlowUpdaterListener(FlowConfigGroup flowConfigGroup, FlowDataSet flowDataSet,
                                                         TrafficCounter trafficCounter, OutputDirectoryHierarchy outputHierarchy) {
        return new FlowUpdaterListner(flowConfigGroup, flowDataSet, trafficCounter, outputHierarchy);
    }
}
