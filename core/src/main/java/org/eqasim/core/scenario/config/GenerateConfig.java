package org.eqasim.core.scenario.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.ModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
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

import ch.ethz.matsim.discrete_mode_choice.modules.ConstraintModule;
import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceConfigurator;
import ch.ethz.matsim.discrete_mode_choice.modules.EstimatorModule;
import ch.ethz.matsim.discrete_mode_choice.modules.ModelModule.ModelType;
import ch.ethz.matsim.discrete_mode_choice.modules.SelectorModule;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.ethz.matsim.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup.HomeType;

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

	public void run(Config config) throws ConfigurationException {
		// Apply modifications
		applyModifications(config);

		// Consider command line
		cmd.applyConfiguration(config);
	}

	protected void applyModifications(Config config) {
		// General settings

		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(40);
		config.controler().setWriteEventsInterval(40);
		config.controler().setWritePlansInterval(40);
		config.controler().setOutputDirectory("simulation_output");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		config.global().setRandomSeed(randomSeed);
		config.global().setNumberOfThreads(threads);

		config.controler().setRoutingAlgorithmType(RoutingAlgorithmType.FastAStarLandmarks);

		config.transit().setUseTransit(true);

		// Disable some standard output
		config.linkStats().setWriteLinkStatsInterval(0);
		config.counts().setWriteCountsInterval(0);

		// QSim settings
		config.qsim().setEndTime(30.0 * 3600.0);
		config.qsim().setNumberOfThreads(Math.min(12, threads));
		config.qsim().setFlowCapFactor(sampleSize);
		config.qsim().setStorageCapFactor(sampleSize);

		// Eqasim settings
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);
		eqasimConfig.setCrossingPenalty(3.0);
		eqasimConfig.setSampleSize(sampleSize);

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

		// Discrete mode choice
		DiscreteModeChoiceConfigurator.configureAsModeChoiceInTheLoop(config);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModelType(ModelType.Tour);
		dmcConfig.setPerformReroute(false);

		dmcConfig.setSelector(SelectorModule.MULTINOMIAL_LOGIT);

		dmcConfig.setTripEstimator(ModeChoiceModule.UTILITY_ESTIMATOR_NAME);
		dmcConfig.setTourEstimator(EstimatorModule.CUMULATIVE);
		dmcConfig.setCachedModes(Arrays.asList("car", "bike", "pt", "walk", "car_passenger", "truck"));

		dmcConfig.setTourFinder(ModeChoiceModule.TOUR_FINDER_NAME);
		dmcConfig.setModeAvailability("unknown");

		dmcConfig.setTourConstraints(
				Arrays.asList(ConstraintModule.VEHICLE_CONTINUITY, ConstraintModule.FROM_TRIP_BASED));
		dmcConfig.setTripConstraints(Arrays.asList(ConstraintModule.TRANSIT_WALK,
				ModeChoiceModule.PASSENGER_CONSTRAINT_NAME, ModeChoiceModule.OUTSIDE_CONSTRAINT_NAME));

		dmcConfig.getVehicleTourConstraintConfig().setHomeType(HomeType.USE_ACTIVITY_TYPE);
		dmcConfig.getVehicleTourConstraintConfig().setRestrictedModes(Arrays.asList("car", "bike"));

		dmcConfig.setTourFilters(
				Arrays.asList(ModeChoiceModule.OUTSIDE_FILTER_NAME, ModeChoiceModule.TOUR_LENGTH_FILTER_NAME));

		// Update utility bindings
		for (String mode : Arrays.asList("car", "pt", "bike", "walk")) {
			eqasimConfig.addModeUtilityMapping(mode, mode);
		}

		for (String mode : Arrays.asList("car_passenger", "truck", "outside")) {
			eqasimConfig.addModeUtilityMapping(mode, "zero");
		}

		// Update paths
		config.network().setInputFile(prefix + "network.xml.gz");
		config.plans().setInputFile(prefix + "population.xml.gz");
		config.households().setInputFile(prefix + "households.xml.gz");
		config.facilities().setInputFile(prefix + "facilities.xml.gz");
		config.transit().setTransitScheduleFile(prefix + "transit_schedule.xml.gz");
		config.transit().setVehiclesFile(prefix + "transit_vehicles.xml.gz");
	}
}
