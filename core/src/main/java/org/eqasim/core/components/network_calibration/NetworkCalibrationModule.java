package org.eqasim.core.components.network_calibration;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.capacities_calibration.CapacitiesAdapter;
import org.eqasim.core.components.network_calibration.capacities_calibration.CountsProcessor;
import org.eqasim.core.components.network_calibration.capacities_calibration.FlowProcessor;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
import org.eqasim.core.components.network_calibration.cost_calibration.RoutingPenaltyByLinkCategory;
import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;

public class NetworkCalibrationModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(NetworkCalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        String objective = config.getObjective();

        if (config.isActivated()) {
            logger.info("Network calibration is activated. Installing components.");
            addEventHandlerBinding().to(TrafficCounter.class).asEagerSingleton();

            if (objective.equals("capacity")) {
                logger.info("Network capacity calibration is activated");
                addControllerListenerBinding().to(CapacitiesAdapter.class).asEagerSingleton();
            } else if (objective.equals("penalty")) {
                logger.info("Network penalties calibration is activated");
                addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("car_passenger").to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("truck").to(EqasimTravelDisutilityFactory.class);

                addControllerListenerBinding().to(PenaltiesAdapter.class).asEagerSingleton();
            }
        } else {
            logger.info("Network calibration is disabled, skipping installation.");
        }
    }

    @Provides
    @Singleton
    EqasimTravelDisutilityFactory providePolicyTravelDisutilityFactory(RoutingPenaltyByLinkCategory linkPenalty) {
        return new EqasimTravelDisutilityFactory(linkPenalty);
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

    @Provides
    @Singleton
    public PenaltiesAdapter providePenaltiesAdapter(CountsProcessor countsProcessor, FlowProcessor flowProcessor, Network network,
                                                    NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                                                    EqasimConfigGroup eqasimConfig) {
        return new PenaltiesAdapter(countsProcessor, flowProcessor, network, config, outputHierarchy, eqasimConfig);
    }

    @Provides
    @Singleton
    public RoutingPenaltyByLinkCategory provideRoutingPenaltyByLinkCategory(PenaltiesAdapter penalties,
                                                                            RoutingPenalty delegate){
        return new RoutingPenaltyByLinkCategory(penalties, delegate);
    }

}
