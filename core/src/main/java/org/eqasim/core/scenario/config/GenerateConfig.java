package org.eqasim.core.scenario.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.ConstraintModule;
import org.matsim.contribs.discrete_mode_choice.modules.DiscreteModeChoiceConfigurator;
import org.matsim.contribs.discrete_mode_choice.modules.EstimatorModule;
import org.matsim.contribs.discrete_mode_choice.modules.ModelModule.ModelType;
import org.matsim.contribs.discrete_mode_choice.modules.SelectorModule;
import org.matsim.contribs.discrete_mode_choice.modules.TourFinderModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup.RoutingAlgorithmType;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

public class GenerateConfig {
	protected final List<String> ACTIVITY_TYPES = Arrays.asList("home", "work", "education", "shop", "leisure", "other",
			"freight_loading", "freight_unloading", "outside");

	protected final List<String> MODES = Arrays.asList("walk", "bike", "pt", "car", "car_passenger", "truck",
			"outside");

	private final List<String> NETWORK_MODES = Arrays.asList("car", "car_passenger", "truck");

	private final CommandLine cmd;
	private final String prefix;
	private final double sampleSize;
	private final int randomSeed;
	private final int threads;

	public GenerateConfig(CommandLine cmd, String prefix, double sampleSize, int randomSeed, int threads) {
		this.sampleSize = sampleSize;
		this.prefix = prefix;
		this.cmd = cmd;
		this.randomSeed = randomSeed;
		this.threads = threads;
	}

	private final static int DEFAULT_ITERATIONS = 60;

	protected void adaptConfiguration(Config config) {
		// General settings

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(DEFAULT_ITERATIONS);
		config.controler().setWriteEventsInterval(DEFAULT_ITERATIONS);
		config.controler().setWritePlansInterval(DEFAULT_ITERATIONS);
		config.controler().setOutputDirectory("simulation_output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.global().setRandomSeed(randomSeed);
		config.global().setNumberOfThreads(threads);

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);

		config.transit().setUseTransit(true);

		// QSim settings
		config.qsim().setEndTime(30.0 * 3600.0);
		config.qsim().setNumberOfThreads(Math.min(12, threads));
		config.qsim().setFlowCapFactor(sampleSize);
		config.qsim().setStorageCapFactor(sampleSize);

		// Eqasim settings
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setCrossingPenalty(3.0);
		eqasimConfig.setSampleSize(sampleSize);
		eqasimConfig.setTripAnalysisInterval(DEFAULT_ITERATIONS);

		// Scoring config
		PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();

		scoringConfig.setMarginalUtilityOfMoney(0.0);
		scoringConfig.setMarginalUtlOfWaitingPt_utils_hr(0.0);

		for (String activityType : ACTIVITY_TYPES) {
			ActivityParams activityParams = scoringConfig.getActivityParams(activityType);

			if (activityParams == null) {
				activityParams = new ActivityParams(activityType);
				config.planCalcScore().addActivityParams(activityParams);
			}

			activityParams.setScoringThisActivityAtAll(false);
		}

		// These parameters are only used by SwissRailRaptor. We configure the
		// parameters here in a way that SRR searches for the route with the shortest
		// travel time.
		for (String mode : MODES) {
			ModeParams modeParams = scoringConfig.getOrCreateModeParams(mode);

			modeParams.setConstant(0.0);
			modeParams.setMarginalUtilityOfDistance(0.0);
			modeParams.setMarginalUtilityOfTraveling(-1.0);
			modeParams.setMonetaryDistanceRate(0.0);
		}

		// Routing configuration
		PlansCalcRouteConfigGroup routingConfig = config.plansCalcRoute();

		config.plansCalcRoute().setNetworkModes(NETWORK_MODES);

		// TODO: Potentially defaults we should change after MATSim 12
		config.plansCalcRoute().setInsertingAccessEgressWalk(false);
		config.plansCalcRoute().setRoutingRandomness(0.0);

		ModeRoutingParams outsideParams = routingConfig.getOrCreateModeRoutingParams("outside");
		outsideParams.setBeelineDistanceFactor(1.0);
		outsideParams.setTeleportedModeSpeed(1000.0);

		ModeRoutingParams bikeParams = routingConfig.getOrCreateModeRoutingParams(TransportMode.bike);
		bikeParams.setBeelineDistanceFactor(1.4);
		bikeParams.setTeleportedModeSpeed(3.1); // 11.6 km/h

		ModeRoutingParams walkParams = routingConfig.getOrCreateModeRoutingParams(TransportMode.walk);
		walkParams.setBeelineDistanceFactor(1.3);
		walkParams.setTeleportedModeSpeed(1.2); // 4.32 km/h

		// Travel time calculator
		config.travelTimeCalculator().setAnalyzedModes(new HashSet<>(NETWORK_MODES));
		config.travelTimeCalculator().setFilterModes(true);
		config.travelTimeCalculator().setSeparateModes(false);

		// Discrete mode choice
		DiscreteModeChoiceConfigurator.configureAsModeChoiceInTheLoop(config, 0.05);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModelType(ModelType.Tour);
		dmcConfig.setPerformReroute(false);

		dmcConfig.setSelector(SelectorModule.MULTINOMIAL_LOGIT);

		dmcConfig.setTripEstimator(EqasimModeChoiceModule.UTILITY_ESTIMATOR_NAME);
		dmcConfig.setTourEstimator(EstimatorModule.CUMULATIVE);
		dmcConfig.setCachedModes(Arrays.asList("car", "bike", "pt", "walk", "car_passenger", "truck"));

		dmcConfig.setTourFinder(TourFinderModule.ACTIVITY_BASED);
		dmcConfig.getActivityTourFinderConfigGroup().setActivityTypes(Arrays.asList("home", "outside"));
		dmcConfig.setModeAvailability("unknown");

		dmcConfig.setTourConstraints(
				Arrays.asList(EqasimModeChoiceModule.VEHICLE_TOUR_CONSTRAINT, ConstraintModule.FROM_TRIP_BASED));
		dmcConfig.setTripConstraints(Arrays.asList(ConstraintModule.TRANSIT_WALK,
				EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME, EqasimModeChoiceModule.OUTSIDE_CONSTRAINT_NAME));

		dmcConfig.setHomeFinder(EqasimModeChoiceModule.HOME_FINDER);
		dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes(Arrays.asList("car", "bike"));

		dmcConfig.setTourFilters(Arrays.asList(EqasimModeChoiceModule.OUTSIDE_FILTER_NAME,
				EqasimModeChoiceModule.TOUR_LENGTH_FILTER_NAME));

		// Set up modes

		eqasimConfig.setEstimator(TransportMode.car, EqasimModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.pt, EqasimModeChoiceModule.PT_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, EqasimModeChoiceModule.BIKE_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.walk, EqasimModeChoiceModule.WALK_ESTIMATOR_NAME);

		for (String mode : Arrays.asList("outside", "car_passenger", "truck")) {
			eqasimConfig.setEstimator(mode, EqasimModeChoiceModule.ZERO_ESTIMATOR_NAME);
		}

		eqasimConfig.setCostModel(TransportMode.car, EqasimModeChoiceModule.ZERO_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, EqasimModeChoiceModule.ZERO_COST_MODEL_NAME);

		// Update paths
		config.network().setInputFile(prefix + "network.xml.gz");
		config.plans().setInputFile(prefix + "population.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");
	}

	public void run(Config config) throws ConfigurationException {
		// Adapt config
		adaptConfiguration(config);

		// Apply command line
		cmd.applyConfiguration(config);
	}
}
