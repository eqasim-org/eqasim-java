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
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.components.network_calibration.capacities.CapacityCorrector;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltiesAdapter;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyManager;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyKeyManager;
import org.eqasim.core.components.network_calibration.cost_calibration.RoutingPenaltyByLinkCategory;
import org.eqasim.core.components.network_calibration.cost_calibration.CostCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.AgentAscsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.CarASCsAdapter;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.ODErrors;
import org.eqasim.core.components.network_calibration.demand_calibration.agent_ascs.PopulationGroups;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.Calibrator;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.config.SubpopulationsCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreeSpeedCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedAdapter;
import org.eqasim.core.components.network_calibration.freespeed_calibration.FreespeedFactorManager;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.network_calibration.freespeed_calibration.TripsHandler;
import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.config.Config;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.router.TripRouter;
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
        validateConfiguration(config);
        boolean costCalibration = config.isCostCalibrationActivated();
        boolean freespeedCalibration = config.isFreeSpeedCalibrationActivated();
        boolean agentCalibration = config.isAgentAscsCalibrationActivated();
        boolean subpopulationsCalibration = config.isSubpopulationsCalibrationActivated();

        if (config.isActivated()) {
            logger.info("Network calibration is activated. Installing components.");

            // 1. Correcting capacities in the network
            bind(CapacityCorrector.class).asEagerSingleton();

            // 2. Install flow module and activate it if it is not activated
            if (config.isCalibrationEnabled() && (costCalibration || agentCalibration || subpopulationsCalibration)) {
                FlowConfigGroup flowConfig = FlowConfigGroup.getOrCreate(getConfig());
                if (!flowConfig.isActivated()) {
                    logger.info("Flow estimation is turned on as part of network calibration.");
                    addEventHandlerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                    addControllerListenerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                }
            }

            // 2. install each component of the calibration module
            if (costCalibration) {
                logger.info("Network penalties calibration is activated");
                addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("car_passenger").to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("truck").to(EqasimTravelDisutilityFactory.class);

                addControllerListenerBinding().to(PenaltiesAdapter.class).asEagerSingleton();
            }

            if (agentCalibration) {
                addControllerListenerBinding().to(CarASCsAdapter.class).asEagerSingleton();
            }

            if (subpopulationsCalibration) {
                addControllerListenerBinding().to(Calibrator.class).asEagerSingleton();
            }

            if (freespeedCalibration) {
                logger.info("Network freespeed calibration is activated");
                addControllerListenerBinding().to(FreespeedAdapter.class).asEagerSingleton();
            }

        } else {
            logger.info("Network calibration is disabled, skipping installation.");
        }
    }

    @Provides
    @Singleton
    EqasimTravelDisutilityFactory providePolicyTravelDisutilityFactory(RoutingPenaltyByLinkCategory linkPenalty, EqasimConfigGroup eqConfig) {
        Config config = getConfig();
        return new EqasimTravelDisutilityFactory(linkPenalty,
                                                 eqConfig.getRoutingDistanceUtility(),
                                                 eqConfig.getSigmaRoutingRandomness(),
                                                 config.global().getRandomSeed());
    }

    @Provides
    @Singleton
    public FlowProcessor provideFlowByLinkCategory(Network network, LinkFlowCounter counter, FlowBinManager flowBinManager,
                                                   CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy,
                                                   EqasimConfigGroup config) {
        double sampleSize = config.getSampleSize();
        return new FlowProcessor(network, counter, flowBinManager, countsProcessor, outputHierarchy, sampleSize);
    }

    @Provides
    @Singleton
    public CountsProcessor provideCountsProcessor(Network network,
                                                  OutputDirectoryHierarchy outputHierarchy,
                                                  LinkCategorizer categorizer,
                                                  PenaltyKeyManager penaltyKeyManager) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        int minObservations = config.getCostCalibrationConfigGroup().getMinObservationsSpecialRegion();
        return new CountsProcessor(network, config, outputHierarchy, categorizer, penaltyKeyManager, minObservations);
    }

    @Provides
    @Singleton
    public PenaltiesAdapter providePenaltiesAdapter(Network network,
                                                    Provider<CountsProcessor> countsProcessorProvider,
                                                    Provider<FlowProcessor> flowProcessorProvider,
                                                    OutputDirectoryHierarchy outputHierarchy,
                                                    EqasimConfigGroup eqasimConfig,
                                                    LinkCategorizer categorizer,
                                                    PenaltyKeyManager penaltyKeyManager,
                                                    PenaltyManager penaltyManager) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        CostCalibrationConfigGroup costConfig = config.getCostCalibrationConfigGroup();
        return new PenaltiesAdapter(network, countsProcessorProvider, flowProcessorProvider, config, costConfig, outputHierarchy,
                eqasimConfig, categorizer, penaltyKeyManager, penaltyManager);
    }

    @Provides
    @Singleton
    public RoutingPenaltyByLinkCategory provideRoutingPenaltyByLinkCategory(PenaltiesAdapter penalties,
                                                                            RoutingPenalty delegate){
        return new RoutingPenaltyByLinkCategory(penalties, delegate);
    }

    @Provides
    @Singleton
    public LinkCategorizer provideLinkCategorizer(Network network) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new LinkCategorizer(network, config);
    }

    @Provides
    @Singleton
    public PenaltyManager providePenaltyManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new PenaltyManager(config, config.getCostCalibrationConfigGroup());
    }

    @Provides
    @Singleton
    public PenaltyKeyManager providePenaltyKeyManager(Network network,
                                                      LinkCategorizer categorizer) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new PenaltyKeyManager(config, config.getCostCalibrationConfigGroup(), network, categorizer);
    }

    @Provides
    @Singleton
    public FreespeedFactorManager provideFreespeedFactorManager() {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FreespeedFactorManager(config, config.getFreeSpeedCalibrationConfigGroup());
    }

    @Provides
    @Singleton
    public FreespeedAdapter provideFreespeedAdapter(Network network,
                                                    OutputDirectoryHierarchy outputHierarchy,
                                                    LinkCategorizer categorizer,
                                                    FreespeedFactorManager factorManager,
                                                    PenaltiesAdapter penaltiesAdapter,
                                                    TripsHandler tripsHandler) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FreespeedAdapter(network, config, config.getFreeSpeedCalibrationConfigGroup(), outputHierarchy, categorizer, factorManager,
                penaltiesAdapter, tripsHandler);
    }

    @Provides
    @Singleton
    public TripsHandler provideTripsHandler(Network network,
                                            LinkCategorizer categorizer,
                                            @Named(TransportMode.car) TravelTime carTravelTime,
                                            Provider<LeastCostPathCalculatorFactory> routerFactoryProvider) {
        int threads = getConfig().global().getNumberOfThreads();
        RoadNetwork roadNetwork = new RoadNetwork(network);
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new TripsHandler(roadNetwork, config, config.getFreeSpeedCalibrationConfigGroup(), categorizer, carTravelTime,
                routerFactoryProvider, threads);
    }

    @Provides
    @Singleton
    public CarASCsAdapter provideASCsAdapter(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, TripListConverter tripListConverter,
                                             OutputDirectoryHierarchy outputHierarchy, Provider<ODErrors> odErrorsProvider, StrategyManager strategyManager) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        double dmcWeight  = getDmcWeight(strategyManager);
        return new CarASCsAdapter(scenario, populationGroupsProvider, tripListConverter, outputHierarchy,
                odErrorsProvider, dmcWeight, config, config.getAgentAscsCalibrationConfigGroup());
    }

    @Provides
    @Singleton
    public ODErrors provideODErrors(Scenario scenario, Provider<PopulationGroups> populationGroupsProvider, Provider<CountsProcessor> countsProcessorProvider,
                                    Provider<FlowProcessor> flowProcessorProvider, TripListConverter tripListConverter, EqasimConfigGroup eqasimConfig) {
        NetworkCalibrationConfigGroup calConfig = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new ODErrors(scenario, populationGroupsProvider, countsProcessorProvider, flowProcessorProvider,
                tripListConverter, eqasimConfig, calConfig, calConfig.getAgentAscsCalibrationConfigGroup());
    }

    @Provides
    @Singleton
    public PopulationGroups providePopulationGroups(Scenario scenario, EqasimConfigGroup config) {
        AgentAscsCalibrationConfigGroup agentConfig = NetworkCalibrationConfigGroup.getOrCreate(getConfig())
                .getAgentAscsCalibrationConfigGroup();
        return PopulationGroups.build(scenario, config.getSampleSize(), false, agentConfig.getInitialCellSize(),
                agentConfig.getMinCellSize(), agentConfig.getMaxPopulationPerCell());
    }

    @Provides
    @Singleton
    public Calibrator provideSubpopulationCalibrationOrchestrator(Scenario scenario,
                                                                  TripListConverter tripListConverter,
                                                                  Provider<CountsProcessor> countsProcessorProvider,
                                                                  Provider<FlowProcessor> flowProcessorProvider,
                                                                  EqasimConfigGroup eqasimConfig,
                                                                  Provider<TripRouter> tripRouterProvider) {
        NetworkCalibrationConfigGroup calConfig = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new Calibrator(
                scenario,
                tripListConverter,
                countsProcessorProvider,
                flowProcessorProvider,
                eqasimConfig,
                calConfig,
                calConfig.getSubpopulationsCalibrationConfigGroup(),
                tripRouterProvider
        );
    }



    static void validateConfiguration(NetworkCalibrationConfigGroup config) {
        if (!config.isActivated()) {
            return;
        }

        List<String> objectives = config.getAllObjectives();
        Set<String> supportedObjectives = Set.of("penalty", "freespeed", "agent", "subpopulations");
        Set<String> invalidObjectives = new HashSet<>();

        for (String objective : objectives) {
            if (!supportedObjectives.contains(objective)) {
                invalidObjectives.add(objective);
            }
        }

        if (!invalidObjectives.isEmpty()) {
            throw new IllegalArgumentException("Unsupported network calibration objective(s): " + invalidObjectives);
        }

        boolean costActive = config.isCostCalibrationActivated();
        boolean freespeedActive = config.isFreeSpeedCalibrationActivated();
        boolean agentActive = config.isAgentAscsCalibrationActivated();
        boolean subpopulationsActive = config.isSubpopulationsCalibrationActivated();
        if (!costActive && !freespeedActive && !agentActive && !subpopulationsActive) {
            throw new IllegalArgumentException("Network calibration is active, but no child calibration group is active.");
        }

        if (config.isCalibrationEnabled() && (costActive || agentActive || subpopulationsActive) && !config.hasCountsFile()) {
            throw new IllegalArgumentException("Cost, agent-ASC, and subpopulation calibration require countsFile.");
        }

        CostCalibrationConfigGroup costConfig = config.getCostCalibrationConfigGroup();
        if (costActive) {
            requirePositive("costCalibration.updateInterval", costConfig.getUpdateInterval());
            requireNonNegative("costCalibration.warmupIterations", costConfig.getWarmupIterations());
            if (costConfig.getMinPenalty() > costConfig.getMaxPenalty()) {
                throw new IllegalArgumentException("costCalibration.minPenalty must be <= maxPenalty.");
            }
            requirePositive("costCalibration.learningRateDecayScale", costConfig.getLearningRateDecayScale());
            requireFraction("costCalibration.signReversalFactor", costConfig.getSignReversalFactor());
            requireFraction("costCalibration.minimumGainMultiplier", costConfig.getMinimumGainMultiplier());
            requireFraction("costCalibration.gainRecoveryRate", costConfig.getGainRecoveryRate());
            requirePositive("costCalibration.minObservationsUrbanRural", costConfig.getMinObservationsUrbanRural());
            requirePositive("costCalibration.minObservationsSpecialRegion", costConfig.getMinObservationsSpecialRegion());
        }

        FreeSpeedCalibrationConfigGroup freespeedConfig = config.getFreeSpeedCalibrationConfigGroup();
        if (freespeedActive) {
            if (config.isCalibrationEnabled() && !freespeedConfig.hasObservedTripsFile()) {
                throw new IllegalArgumentException("Freespeed calibration requires freespeedCalibration.observedTripsFile.");
            }
            requirePositive("freespeedCalibration.updateInterval", freespeedConfig.getUpdateInterval());
            requireNonNegative("freespeedCalibration.warmupIterations", freespeedConfig.getWarmupIterations());
            if (freespeedConfig.getMinFactor() <= 0.0 || freespeedConfig.getMinFactor() > freespeedConfig.getMaxFactor()) {
                throw new IllegalArgumentException("freespeedCalibration.minFactor must be > 0 and <= maxFactor.");
            }
            if (freespeedConfig.getTrimFraction() < 0.0 || freespeedConfig.getTrimFraction() >= 0.5) {
                throw new IllegalArgumentException("freespeedCalibration.trimFraction must be in [0, 0.5).");
            }
            requirePositive("freespeedCalibration.frozenIterations", freespeedConfig.getFrozenIterations());
            requirePositive("freespeedCalibration.historySize", freespeedConfig.getHistorySize());
            requireNonNegative("freespeedCalibration.unboundedInitialUpdates", freespeedConfig.getUnboundedInitialUpdates());
            requirePositive("freespeedCalibration.noImprovementPatience", freespeedConfig.getNoImprovementPatience());
            if (freespeedConfig.getMinEffectiveLearningRate() > freespeedConfig.getMaxEffectiveLearningRate()) {
                throw new IllegalArgumentException("freespeedCalibration.minEffectiveLearningRate must be <= maxEffectiveLearningRate.");
            }
        }

        AgentAscsCalibrationConfigGroup agentConfig = config.getAgentAscsCalibrationConfigGroup();
        if (agentActive) {
            requireNonNegative("agentAscsCalibration.warmupIterations", agentConfig.getWarmupIterations());
            int rebuildCount = agentConfig.getGridRebuildUpdates().size();
            if (agentConfig.getGridRebuildInitialCellSizes().size() != rebuildCount
                    || agentConfig.getGridRebuildMinCellSizes().size() != rebuildCount
                    || agentConfig.getGridRebuildMaxPopulations().size() != rebuildCount) {
                throw new IllegalArgumentException("All agentAscsCalibration grid-rebuild lists must have equal length.");
            }
            requirePositive("agentAscsCalibration.initialCellSize", agentConfig.getInitialCellSize());
            requirePositive("agentAscsCalibration.minCellSize", agentConfig.getMinCellSize());
            requirePositive("agentAscsCalibration.maxPopulationPerCell", agentConfig.getMaxPopulationPerCell());
        }

        SubpopulationsCalibrationConfigGroup subpopulationsConfig = config.getSubpopulationsCalibrationConfigGroup();
        if (subpopulationsActive) {
            requirePositive("subpopulationsCalibration.updateInterval", subpopulationsConfig.getUpdateInterval());
            requirePositive("subpopulationsCalibration.earlyUpdateInterval", subpopulationsConfig.getEarlyUpdateInterval());
            requireNonNegative("subpopulationsCalibration.warmupIterations", subpopulationsConfig.getWarmupIterations());
            requirePositive("subpopulationsCalibration.freightRelocationRadiusFactor", subpopulationsConfig.getBackgroundRelocationRadiusFactor());
            requirePositive("subpopulationsCalibration.freightMinimumRadius", subpopulationsConfig.getBackgroundMinimumRadius());
            requirePositive("subpopulationsCalibration.freightMaximumRadius", subpopulationsConfig.getBackgroundMaximumRadius());
            requireFraction("subpopulationsCalibration.freightRelocationTryFraction",
                    subpopulationsConfig.getBackgroundRelocationTryFraction());
            requireFraction("subpopulationsCalibration.destinationSelectionProbability",
                    subpopulationsConfig.getDestinationSelectionProbability());
            if (subpopulationsConfig.isCrossBorderCalibrationEnabled()) {
                requirePositive("subpopulationsCalibration.relocationRadius", subpopulationsConfig.getRelocationRadius());
                requirePositive("subpopulationsCalibration.homeRelocationRadius", subpopulationsConfig.getHomeRelocationRadius());
                requireNonNegative("subpopulationsCalibration.maximumTimeShift", subpopulationsConfig.getMaximumTimeShift());
                requirePositiveFraction("subpopulationsCalibration.crossBorderShareThreshold",
                        subpopulationsConfig.getCrossBorderShareThreshold());
                requirePositiveFraction("subpopulationsCalibration.crossBorderUpdateFraction",
                        subpopulationsConfig.getCrossBorderUpdateFraction());
            }
            if (subpopulationsConfig.getBackgroundMinimumRadius() > subpopulationsConfig.getBackgroundMaximumRadius()) {
                throw new IllegalArgumentException("subpopulationsCalibration.freightMinimumRadius must be <= freightMaximumRadius.");
            }
        }
    }

    private static void requirePositive(String name, int value) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be > 0.");
    }

    private static void requireNonNegative(String name, int value) {
        if (value < 0) throw new IllegalArgumentException(name + " must be >= 0.");
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0) throw new IllegalArgumentException(name + " must be finite and > 0.");
    }

    private static void requireFraction(String name, double value) {
        if (!Double.isFinite(value) || value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be in [0, 1].");
        }
    }

    private static void requirePositiveFraction(String name, double value) {
        if (!Double.isFinite(value) || value <= 0.0 || value > 1.0) {
            throw new IllegalArgumentException(name + " must be in (0, 1].");
        }
    }

    static double getDmcWeight(StrategyManager strategyManager) {
        List<GenericPlanStrategy<Plan, Person>> strategies = strategyManager.getStrategies(null);
        List<Double> weights = strategyManager.getWeights(null);
        for (GenericPlanStrategy<Plan, Person> strategy : strategies) {
            if (strategy.toString().contains("DiscreteModeChoice")) {
                return weights.get(strategies.indexOf(strategy));
            }
        }
        return 0.05; // default weight if DMCStrategy is not found
    }

}
