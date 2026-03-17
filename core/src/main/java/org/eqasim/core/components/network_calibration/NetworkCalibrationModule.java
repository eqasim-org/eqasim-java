package org.eqasim.core.components.network_calibration;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.flow.FlowConfigGroup;
import org.eqasim.core.components.network_calibration.capacities_calibration.CapacitiesAdapter;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyManager;
import org.eqasim.core.components.network_calibration.cost_calibration.RoutingPenaltyByLinkCategory;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedAdapter;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedFactorManager;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.TravelTime;

import java.util.List;

public class NetworkCalibrationModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(NetworkCalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        List<String> objectives = config.getAllObjectives();

        // throw an error if both penalties and capacities are targeted, this is not good as their interaction does not converge in the current state of the code
        if (objectives.contains("capacity") & objectives.contains("penalty") ){
            throw new RuntimeException("Both capacity and penalty calibration are activated. This is not recommended as their " +
                    "interaction does not converge in the current state of the code. Please choose one of them or run them "+
                    "sequentially. If you decide to run them sequentially, it is recommended to run the penalty calibration first. ");
        }


        if (config.isActivated()) {
            logger.info("Network calibration is activated. Installing components.");

            // 1. Install flow module and activate it if it is not activated
            if (objectives.contains("capacity") || objectives.contains("penalty")) {
                FlowConfigGroup flowConfig = FlowConfigGroup.getOrCreate(getConfig());
                if (!flowConfig.isActivated()) {
                    logger.info("Flow estimation is turned on as part of network calibration.");
                    addEventHandlerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                    addControllerListenerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                }
            }

            // 2. install each component of the calibration module
            if (objectives.contains("capacity")) {
                logger.info("Network capacity calibration is activated");
                addControllerListenerBinding().to(CapacitiesAdapter.class).asEagerSingleton();
            }

            if (objectives.contains("penalty")) {
                logger.info("Network penalties calibration is activated");
                addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("car_passenger").to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("truck").to(EqasimTravelDisutilityFactory.class);

                addControllerListenerBinding().to(PenaltiesAdapter.class).asEagerSingleton();
            }

            if (objectives.contains("freespeed")) {
                logger.info("Network freespeed calibration is activated");
                addControllerListenerBinding().to(FreespeedAdapter.class).asEagerSingleton();
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
    public FlowProcessor provideFlowByLinkCategory(Network network, LinkFlowCounter counter, FlowBinManager flowBinManager,
                                                   CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FlowProcessor(network, counter, flowBinManager, countsProcessor, outputHierarchy, config);
    }

    @Provides
    @Singleton
    public CapacitiesAdapter provideCapacitiesAdapter(Network network, FlowProcessor flowsEstimator,
                                                      CountsProcessor countsProcessor,
                                                      NetworkCalibrationConfigGroup config,
                                                      EqasimConfigGroup eqasimConfig,
                                                      OutputDirectoryHierarchy outputHierarchy,
                                                      LinkCategorizer categorizer) {
        return new CapacitiesAdapter(network, flowsEstimator, countsProcessor, config, eqasimConfig, outputHierarchy, categorizer);
    }

    @Provides
    @Singleton
    public CountsProcessor provideCountsProcessor(Network network, OutputDirectoryHierarchy outputHierarchy, LinkCategorizer categorizer) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new CountsProcessor(network, config, outputHierarchy, categorizer);
    }

    @Provides
    @Singleton
    public PenaltiesAdapter providePenaltiesAdapter(CountsProcessor countsProcessor, FlowProcessor flowProcessor, Network network,
                                                    NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                                                    EqasimConfigGroup eqasimConfig,
                                                    LinkCategorizer categorizer,
                                                    PenaltyManager penaltyManager) {
        return new PenaltiesAdapter(countsProcessor, flowProcessor, network, config, outputHierarchy, eqasimConfig, categorizer, penaltyManager);
    }

    @Provides
    @Singleton
    public RoutingPenaltyByLinkCategory provideRoutingPenaltyByLinkCategory(PenaltiesAdapter penalties,
                                                                            RoutingPenalty delegate){
        return new RoutingPenaltyByLinkCategory(penalties, delegate);
    }

    @Provides
    @Singleton
    public LinkCategorizer provideLinkCategorizer() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new LinkCategorizer(config.getSeparateUrbanRoads());
    }

    @Provides
    @Singleton
    public PenaltyManager providePenaltyManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new PenaltyManager(config.getMinPenalty(), config.getMaxPenalty(), config.getCalibrate());
    }

    @Provides
    @Singleton
    public FreespeedFactorManager provideFreespeedFactorManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FreespeedFactorManager(
                config.getMinFreespeedFactor(),
                config.getMaxFreespeedFactor(),
                config.getBeta(),
                config.getCalibrate(),
                config.getMinTripsPerGroup()
        );
    }

    @Provides
    @Singleton
    public FreespeedAdapter provideFreespeedAdapter(Network network,
                                                    NetworkCalibrationConfigGroup config,
                                                    OutputDirectoryHierarchy outputHierarchy,
                                                    LinkCategorizer categorizer,
                                                    FreespeedFactorManager factorManager,
                                                    @Named(TransportMode.car) TravelTime carTravelTime) {
        int threads = getConfig().global().getNumberOfThreads();
        return new FreespeedAdapter(network, config, outputHierarchy, categorizer, factorManager, carTravelTime, threads);
    }

}
