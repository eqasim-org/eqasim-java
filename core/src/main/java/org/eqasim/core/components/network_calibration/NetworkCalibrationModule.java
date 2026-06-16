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
import org.eqasim.core.components.network_calibration.demand_calibration.CarASCsAdapter;
import org.eqasim.core.components.network_calibration.demand_calibration.ODErrors;
import org.eqasim.core.components.network_calibration.demand_calibration.PopulationGroups;
import org.eqasim.core.components.network_calibration.demand_calibration.SubPopulationReducer;
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
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;
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

            // 1. Correcting capacities in the network
            bind(CapacityCorrector.class).asEagerSingleton();

            // 2. Install flow module and activate it if it is not activated
            if (config.isCalibrationEnabled() && objectives.contains("penalty")) {
                FlowConfigGroup flowConfig = FlowConfigGroup.getOrCreate(getConfig());
                if (!flowConfig.isActivated()) {
                    logger.info("Flow estimation is turned on as part of network calibration.");
                    addEventHandlerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                    addControllerListenerBinding().to(LinkFlowCounter.class).asEagerSingleton();
                }
            }

            // 2. install each component of the calibration module
            if (objectives.contains("penalty")) {
                logger.info("Network penalties calibration is activated");
                addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("car_passenger").to(EqasimTravelDisutilityFactory.class);
                addTravelDisutilityFactoryBinding("truck").to(EqasimTravelDisutilityFactory.class);

                addControllerListenerBinding().to(PenaltiesAdapter.class).asEagerSingleton();
            }

            if (objectives.contains("agent")){
                addControllerListenerBinding().to(CarASCsAdapter.class).asEagerSingleton();
            }

            if (objectives.contains("subpopulations")){
                addControllerListenerBinding().to(SubPopulationReducer.class).asEagerSingleton();
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
    EqasimTravelDisutilityFactory providePolicyTravelDisutilityFactory(RoutingPenaltyByLinkCategory linkPenalty, EqasimConfigGroup config) {
        return new EqasimTravelDisutilityFactory(linkPenalty, config.getRoutingDistanceUtility());
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
        return new CountsProcessor(network, config, outputHierarchy, categorizer, penaltyKeyManager);
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
        return new PenaltiesAdapter(network, countsProcessorProvider, flowProcessorProvider, config, outputHierarchy,
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
        return new PenaltyManager(config);
    }

    @Provides
    @Singleton
    public PenaltyKeyManager providePenaltyKeyManager(Network network,
                                                      LinkCategorizer categorizer) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new PenaltyKeyManager(config, network, categorizer);
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
                                                    OutputDirectoryHierarchy outputHierarchy,
                                                    LinkCategorizer categorizer,
                                                    FreespeedFactorManager factorManager,
                                                    PenaltiesAdapter penaltiesAdapter,
                                                    TripsHandler tripsHandler) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        return new FreespeedAdapter(network, config, outputHierarchy, categorizer, factorManager,
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
        return new TripsHandler(roadNetwork, config, categorizer, carTravelTime,
                routerFactoryProvider, threads);
    }

    @Provides
    @Singleton
    public CarASCsAdapter provideASCsAdapter(Scenario scenario, PopulationGroups populationGroups, TripListConverter tripListConverter,
                                             OutputDirectoryHierarchy outputHierarchy, ODErrors odErrors, StrategyManager strategyManager) {
        NetworkCalibrationConfigGroup config = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        boolean activate  = config.getAllObjectives().contains("agent");
        double dmcWeight  = getDmcWeight(strategyManager);
        return new CarASCsAdapter(scenario, populationGroups, tripListConverter, outputHierarchy, odErrors, dmcWeight, activate);
    }

    @Provides
    @Singleton
    public ODErrors provideODErrors(Scenario scenario, PopulationGroups populationGroups, CountsProcessor countsProcessor,
                                       FlowProcessor flowProcessor, TripListConverter tripListConverter, EqasimConfigGroup config) {
        double sampleSize = config.getSampleSize();
        return new ODErrors(scenario, populationGroups, countsProcessor, flowProcessor, tripListConverter, sampleSize);
    }

    @Provides
    @Singleton
    public PopulationGroups providePopulationGroups(Scenario scenario, EqasimConfigGroup config) {
        return PopulationGroups.build(scenario, config.getSampleSize(), false);
    }

    @Provides
    @Singleton
    public SubPopulationReducer provideSubPopulationReducer(Scenario scenario, TripListConverter tripListConverter,
                                                            CountsProcessor countsProcessor, FlowProcessor flowProcessor,
                                                            EqasimConfigGroup config) {
        NetworkCalibrationConfigGroup calConfig = NetworkCalibrationConfigGroup.getOrCreate(getConfig());
        boolean activate  = calConfig.getAllObjectives().contains("subpopulations");
        return new SubPopulationReducer(scenario.getPopulation(), tripListConverter, countsProcessor,
                flowProcessor, config.getSampleSize(), activate);
    }



    static void validateConfiguration(NetworkCalibrationConfigGroup config) {
        if (!config.isActivated()) {
            return;
        }

        List<String> objectives = config.getAllObjectives();
        if (objectives.isEmpty()) {
            throw new IllegalArgumentException("Network calibration is activated but objective is empty. Supported objectives are: penalty, freespeed.");
        }

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

        boolean calibrate = config.isCalibrationEnabled();
        boolean hasPenaltyObjective = objectives.contains("penalty");
        boolean hasFreespeedObjective = objectives.contains("freespeed");

        if (calibrate && hasPenaltyObjective && !config.hasCountsFile()) {
            throw new IllegalArgumentException("Penalty calibration requires countsFile.");
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
