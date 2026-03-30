package org.eqasim.core.components.network_calibration;

import com.google.inject.Provides;
import com.google.inject.Provider;
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
import org.eqasim.core.components.network_calibration.freespeed_calibration.TripsHandler;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NetworkCalibrationModule extends AbstractEqasimExtension {

    private static final Logger logger = LogManager.getLogger(NetworkCalibrationModule.class);

    @Override
    protected void installEqasimExtension() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        List<String> objectives = config.getAllObjectives();
        validateConfiguration(config);

        if (config.isActivated()) {
            logger.info("Network calibration is activated. Installing components.");

            // 1. Install flow module and activate it if it is not activated
            if (config.isCalibrationEnabled() && (objectives.contains("capacity") || objectives.contains("penalty"))) {
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
    public CapacitiesAdapter provideCapacitiesAdapter(Network network,
                                                      Provider<FlowProcessor> flowProcessorProvider,
                                                      Provider<CountsProcessor> countsProcessorProvider,
                                                      NetworkCalibrationConfigGroup config,
                                                      EqasimConfigGroup eqasimConfig,
                                                      OutputDirectoryHierarchy outputHierarchy,
                                                      LinkCategorizer categorizer) {
        return new CapacitiesAdapter(network, flowProcessorProvider, countsProcessorProvider, config, eqasimConfig, outputHierarchy, categorizer);
    }

    @Provides
    @Singleton
    public CountsProcessor provideCountsProcessor(Network network, OutputDirectoryHierarchy outputHierarchy, LinkCategorizer categorizer) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new CountsProcessor(network, config, outputHierarchy, categorizer);
    }

    @Provides
    @Singleton
    public PenaltiesAdapter providePenaltiesAdapter(Provider<CountsProcessor> countsProcessorProvider,
                                                    Provider<FlowProcessor> flowProcessorProvider,
                                                    NetworkCalibrationConfigGroup config, OutputDirectoryHierarchy outputHierarchy,
                                                    EqasimConfigGroup eqasimConfig,
                                                    LinkCategorizer categorizer,
                                                    PenaltyManager penaltyManager) {
        return new PenaltiesAdapter(countsProcessorProvider, flowProcessorProvider, config, outputHierarchy, eqasimConfig, categorizer, penaltyManager);
    }

    @Provides
    @Singleton
    public RoutingPenaltyByLinkCategory provideRoutingPenaltyByLinkCategory(PenaltiesAdapter penalties,
                                                                            RoutingPenalty delegate){
        return new RoutingPenaltyByLinkCategory(penalties, delegate);
    }

    @Provides
    @Singleton
    public LinkCategorizer provideLinkCategorizer(Network network, NetworkCalibrationConfigGroup config) {
        return new LinkCategorizer(network, config);
    }

    @Provides
    @Singleton
    public PenaltyManager providePenaltyManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new PenaltyManager(config.getMinPenalty(), config.getMaxPenalty(), config.isCalibrationEnabled());
    }

    @Provides
    @Singleton
    public FreespeedFactorManager provideFreespeedFactorManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FreespeedFactorManager(config);
    }

    @Provides
    @Singleton
    public FreespeedAdapter provideFreespeedAdapter(Network network,
                                                    NetworkCalibrationConfigGroup config,
                                                    OutputDirectoryHierarchy outputHierarchy,
                                                    LinkCategorizer categorizer,
                                                    FreespeedFactorManager factorManager,
                                                    PenaltiesAdapter penaltiesAdapter,
                                                    TripsHandler tripsHandler) {

        return new FreespeedAdapter(network, config, outputHierarchy, categorizer, factorManager,
                penaltiesAdapter, tripsHandler);
    }

    @Provides
    @Singleton
    public TripsHandler provideTripsHandler(Network network,
                                            NetworkCalibrationConfigGroup config,
                                            LinkCategorizer categorizer,
                                            @Named(TransportMode.car) TravelTime carTravelTime,
                                            Provider<LeastCostPathCalculatorFactory> routerFactoryProvider) {
        int threads = getConfig().global().getNumberOfThreads();
        RoadNetwork roadNetwork = new RoadNetwork(network);
        return new TripsHandler(roadNetwork, config, categorizer, carTravelTime,
                routerFactoryProvider, threads);
    }

    static void validateConfiguration(NetworkCalibrationConfigGroup config) {
        if (!config.isActivated()) {
            return;
        }

        List<String> objectives = config.getAllObjectives();
        Set<String> supportedObjectives = Set.of("capacity", "penalty", "freespeed");
        Set<String> invalidObjectives = new HashSet<>();

        for (String objective : objectives) {
            if (!supportedObjectives.contains(objective)) {
                invalidObjectives.add(objective);
            }
        }

        if (!invalidObjectives.isEmpty()) {
            throw new IllegalArgumentException("Unsupported network calibration objective(s): " + invalidObjectives);
        }

        boolean calibrate = config.isCalibrationEnabled();
        boolean hasCapacityObjective = objectives.contains("capacity");
        boolean hasPenaltyObjective = objectives.contains("penalty");
        boolean hasFreespeedObjective = objectives.contains("freespeed");

        if (calibrate && hasCapacityObjective && hasPenaltyObjective) {
            throw new IllegalArgumentException("Both capacity and penalty calibration are enabled. Please calibrate them sequentially.");
        }

        if (calibrate && (hasCapacityObjective || hasPenaltyObjective)
                && !config.hasCountsFile() && !config.hasAverageCountsPerCategoryFile()) {
            throw new IllegalArgumentException("Calibration of capacity/penalty requires countsFile or averageCountsPerCategoryFile.");
        }

        if (!calibrate && hasCapacityObjective && !config.hasCapacitiesFile()) {
            throw new IllegalArgumentException("When objective includes capacity and calibrate=false, capacitiesFile must be provided.");
        }

        if (!calibrate && hasPenaltyObjective && !config.hasPenaltiesFile()) {
            throw new IllegalArgumentException("When objective includes penalty and calibrate=false, penaltiesFile must be provided.");
        }

        if (calibrate && hasFreespeedObjective && !config.hasObservedSpeedTripsFile()) {
            throw new IllegalArgumentException("Freespeed calibration requires observedSpeedTripsFile.");
        }

        if (hasFreespeedObjective) {
            if (config.getMinFreespeedFactor() <= 0.0 || config.getMaxFreespeedFactor() <= 0.0
                    || config.getMinFreespeedFactor() > config.getMaxFreespeedFactor()) {
                throw new IllegalArgumentException("Invalid freespeed factor bounds: minFreespeedFactor must be > 0 and <= maxFreespeedFactor.");
            }

            if (config.getFreespeedWarmupIterations() < 0) {
                throw new IllegalArgumentException("freespeedWarmupIterations must be >= 0.");
            }

        }

        if (!calibrate && hasFreespeedObjective && !config.hasFreespeedFactorsFile()) {
            throw new IllegalArgumentException("When objective includes freespeed and calibrate=false, freespeedFactorsFile must be provided.");
        }
    }

}
