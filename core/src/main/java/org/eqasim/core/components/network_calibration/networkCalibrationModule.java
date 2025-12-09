package org.eqasim.core.components.network_calibration;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.capacities_calibration.CapacitiesAdapter;
import org.eqasim.core.components.network_calibration.capacities_calibration.CountsProcessor;
import org.eqasim.core.components.network_calibration.capacities_calibration.FlowProcessor;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class networkCalibrationModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(networkCalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        if (config.isActivated()) {
            logger.info("Network calibration is activated. Installing components.");
            addEventHandlerBinding().to(TrafficCounter.class).asEagerSingleton();
            addControllerListenerBinding().to(CapacitiesAdapter.class).asEagerSingleton();
        } else {
            logger.info("Network calibration is disabled, skipping installation.");
        }
    }

    @Provides
    @Singleton
    public TrafficCounter provideTrafficCounter(Network network, FlowDataSet flowDataSet, TimeBinManager timeBinManager,
                                                OutputDirectoryHierarchy outputHierarchy) {
        DelaysConfigGroup config = DelaysConfigGroup.getOrCreate(getConfig());
        return new TrafficCounter(network, flowDataSet, timeBinManager, outputHierarchy, config);
    }

    @Provides
    @Singleton
    public TimeBinManager provideTimeBinManager() {
        DelaysConfigGroup config = DelaysConfigGroup.getOrCreate(getConfig());
        return new TimeBinManager(config);
    }

    @Provides
    @Singleton
    public FlowProcessor provideFlowByLinkCategory(Network network, TrafficCounter counter, TimeBinManager timeBinManager,
                                                   CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FlowProcessor(network, counter, timeBinManager, countsProcessor, outputHierarchy, config);
    }

    @Provides
    @Singleton
    public CapacitiesAdapter provideCapacitiesAdapter(Network network, FlowProcessor flowsEstimator,
                                                      CountsProcessor countsProcessor,
                                                      NetworkCalibrationConfigGroup config,
                                                      EqasimConfigGroup eqasimConfig,
                                                      OutputDirectoryHierarchy outputHierarchy) {
        return new CapacitiesAdapter(network, flowsEstimator, countsProcessor, config, eqasimConfig, outputHierarchy);
    }

    @Provides
    @Singleton
    public CountsProcessor provideCountsProcessor(Network network, OutputDirectoryHierarchy outputHierarchy) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new CountsProcessor(network, config, outputHierarchy);
    }

}
