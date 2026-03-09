package org.eqasim.core.components.flow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class FlowModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(FlowModule.class);

    @Override
    protected void installEqasimExtension() {
        FlowConfigGroup config = FlowConfigGroup.getOrCreate(getConfig());
        if (config.isActivated()) {
            logger.info("Flow module is activated, installing components.");
            // This part manages the flow data collection
            addEventHandlerBinding().to(LinkFlowCounter.class).asEagerSingleton();
            addControllerListenerBinding().to(LinkFlowCounter.class).asEagerSingleton();
        } else {
            logger.info("Flow module is not activated, skipping installation of components.");
        }
    }


    @Provides
    @Singleton
    public FlowBinManager provideFlowBinManager(FlowConfigGroup config) {
        return new FlowBinManager(config);
    }

    @Provides
    @Singleton
    public FlowDataSet provideFlowDataSet(Network network, FlowBinManager timeBinManager, FlowConfigGroup config) {
        return new FlowDataSet(network, timeBinManager, config.getBeta());
    }

    @Provides
    @Singleton
    public LinkFlowCounter provideTrafficCounter(Network network, FlowDataSet flowDataSet, FlowBinManager timeBinManager, Scenario scenario,
                                                 OutputDirectoryHierarchy outputHierarchy, FlowConfigGroup flowConfig) {
        return new LinkFlowCounter(network, flowDataSet, timeBinManager, outputHierarchy, flowConfig, scenario);
    }



}
