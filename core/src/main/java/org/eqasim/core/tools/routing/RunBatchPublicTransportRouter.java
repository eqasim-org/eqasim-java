package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.components.headway.HeadwayImputerModule;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.LegInformation;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.Task;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.TripInformation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.MatsimVehicleReader;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Injector;
import com.google.inject.Provider;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class RunBatchPublicTransportRouter {
	private final static Logger logger = LogManager.getLogger(RunBatchPublicTransportRouter.class);

	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path") //
				.allowOptions("threads", "batch-size", "interval", //
						"transfer-utility", "waiting-utility", //
						"direct-walk-factor", "maximum-transfer-distance", //
						"walk-factor", "walk-speed", //
						"output-trips-path", "output-legs-path", "output-config-path") //
				.allowPrefixes("travel-utility") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		// No opportunity scoring
		config.planCalcScore().setPerforming_utils_hr(0.0);

		if (config.planCalcScore().getPerforming_utils_hr() != 0.0) {
			logger.warn("Setting opporunity cost to zero");
		}

		if (cmd.hasOption("maximum-transfer-distance")) {
			config.transitRouter().setMaxBeelineWalkConnectionDistance(
					Double.parseDouble(cmd.getOptionStrict("maximum-transfer-distance")));
			logger.info("Setting maximum transfer distance to "
					+ config.transitRouter().getMaxBeelineWalkConnectionDistance());
		}

		// Transfer utility
		if (cmd.hasOption("transfer-utility")) {
			config.planCalcScore().setUtilityOfLineSwitch(Double.parseDouble(cmd.getOptionStrict("transfer-utility")));
			logger.info("Setting transfer utility to " + config.planCalcScore().getUtilityOfLineSwitch());
		}

		// Waiting utility
		if (cmd.hasOption("waiting-utility")) {
			config.planCalcScore()
					.setMarginalUtlOfWaitingPt_utils_hr(Double.parseDouble(cmd.getOptionStrict("waiting-utility")));
			logger.info("Setting waiting utility to " + config.planCalcScore().getMarginalUtlOfWaitingPt_utils_hr());
		}

		// Direct walk factor
		if (cmd.hasOption("direct-walk-factor")) {
			config.transitRouter().setDirectWalkFactor(Double.parseDouble(cmd.getOptionStrict("direct-walk-factor")));
			logger.info("Setting direct walk factor to " + config.transitRouter().getDirectWalkFactor());
		}

		// Walking
		ModeRoutingParams walkRoutingParams = config.plansCalcRoute().getModeRoutingParams().get("walk");

		if (cmd.hasOption("walk-factor")) {
			walkRoutingParams.setBeelineDistanceFactor(Double.parseDouble(cmd.getOptionStrict("walk-factor")));
			logger.info("Setting walk factor to " + walkRoutingParams.getBeelineDistanceFactor());
		}

		if (cmd.hasOption("walk-speed")) {
			walkRoutingParams.setTeleportedModeSpeed(Double.parseDouble(cmd.getOptionStrict("walk-speed")));
			logger.info("Setting walk speed to " + walkRoutingParams.getTeleportedModeSpeed());
		}

		// Load scenario to find transit modes
		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);

		// We only load network, schedule and transit vehicles
		new MatsimNetworkReader(scenario.getNetwork())
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.network().getInputFile()));
		new TransitScheduleReader(scenario)
				.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getTransitScheduleFile()));

		if (config.transit().getVehiclesFile() != null) {
			new MatsimVehicleReader(scenario.getTransitVehicles())
					.readURL(ConfigGroup.getInputFileURL(config.getContext(), config.transit().getVehiclesFile()));
		}

		// Collect travel utilities
		Set<String> transitModes = new HashSet<>();
		for (TransitLine transitLine : scenario.getTransitSchedule().getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				transitModes.add(transitRoute.getTransportMode());
			}
		}

		Map<String, Double> utilities = new HashMap<>();

		for (String option : cmd.getAvailableOptions()) {
			if (option.startsWith("travel-utility:")) {
				String[] parts = option.split(":");
				String mode = parts[1];
				utilities.put(mode, Double.parseDouble(cmd.getOptionStrict(option)));
			}
		}

		if (utilities.size() > 0) {
			logger.info("Updating utilities");

			Set<String> modeSpecificTesting = new HashSet<>();
			modeSpecificTesting.addAll(utilities.keySet());
			modeSpecificTesting.remove("walk");
			modeSpecificTesting.remove("pt");

			SwissRailRaptorConfigGroup srrConfig = (SwissRailRaptorConfigGroup) config.getModules()
					.get(SwissRailRaptorConfigGroup.GROUP);

			if (modeSpecificTesting.size() == 0) {
				// Not mode specific
				logger.info("Setting up mode *un*specific routing");
				srrConfig.setUseModeMappingForPassengers(false);

				for (String mode : utilities.keySet()) {
					ModeParams modeParameters = config.planCalcScore().getModes().get(mode);
					modeParameters.setMarginalUtilityOfTraveling(utilities.get(mode));
					logger.info(String.format("Setting travel utility for %s to %f", mode,
							modeParameters.getMarginalUtilityOfTraveling()));
				}
			} else {
				logger.info("Setting up mode *specific* routing");
				srrConfig.setUseModeMappingForPassengers(true);

				Set<String> availableModes = new HashSet<>();
				availableModes.addAll(transitModes);
				availableModes.add("other");
				availableModes.add("walk");

				for (String mode : utilities.keySet()) {
					if (!availableModes.contains(mode)) {
						throw new IllegalStateException("No need to set travel utility for mode: " + mode);
					}

					ModeParams modeParameters = config.planCalcScore().getModes().get(mode);

					if (modeParameters == null) {
						modeParameters = new ModeParams(mode);
						modeParameters.setConstant(0.0);
						modeParameters.setMarginalUtilityOfDistance(0.0);
						modeParameters.setMonetaryDistanceRate(0.0);
						config.planCalcScore().addModeParams(modeParameters);
					}

					modeParameters.setMarginalUtilityOfTraveling(utilities.get(mode));
					logger.info(String.format("Setting travel utility for %s to %f", mode,
							modeParameters.getMarginalUtilityOfTraveling()));

					SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet mapping = null;

					for (var candidate : srrConfig.getModeMappingForPassengers()) {
						if (candidate.getRouteMode().equals(mode)) {
							mapping = candidate;
							break;
						}
					}

					if (mapping == null) {
						srrConfig.addModeMappingForPassengers(
								new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet(mode, mode));
					} else {
						mapping.setPassengerMode(mode);
					}
				}

				for (String mode : availableModes) {
					ModeParams modeParameters = config.planCalcScore().getModes().get(mode);

					if (modeParameters == null) {
						double utility = utilities.getOrDefault("other", -1.0);

						modeParameters = new ModeParams(mode);
						modeParameters.setConstant(0.0);
						modeParameters.setMarginalUtilityOfDistance(0.0);
						modeParameters.setMonetaryDistanceRate(0.0);
						modeParameters.setMarginalUtilityOfTraveling(utility);
						config.planCalcScore().addModeParams(modeParameters);

						logger.info(String.format("Creating mode with travel utility for %s as %f", mode,
								modeParameters.getMarginalUtilityOfTraveling()));

						srrConfig.addModeMappingForPassengers(
								new SwissRailRaptorConfigGroup.ModeMappingForPassengersParameterSet(mode, mode));
					}
				}
			}
		}

		/*-{
			RaptorParameters raptorParameters = RaptorUtils.createParameters(config);
		
			Scenario scenario;
		
			OccupancyData occupancyData = new OccupancyData();
			RaptorStaticConfig srrStaticConfig = RaptorUtils.createStaticConfig(config);
		
			SwissRailRaptorData srrData = SwissRailRaptorData.create(scenario.getTransitSchedule(),
					scenario.getTransitVehicles(), srrStaticConfig, scenario.getNetwork(), occupancyData);
			SwissRailRaptor raptor = new SwissRailRaptor.Builder(srrData, config).build();
		
		}-*/

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		double interval = (double) cmd.getOption("interval").map(Integer::parseInt).orElse(0);

		Optional<String> outputLegsPath = cmd.getOption("output-legs-path");
		Optional<String> outputTripsPath = cmd.getOption("output-trips-path");
		Optional<String> outputConfigPath = cmd.getOption("output-config-path");

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new HeadwayImputerModule(numberOfThreads, batchSize, false, interval)).build();

		Provider<TransitRouter> routerProvider = injector.getProvider(TransitRouter.class);
		Provider<HeadwayCalculator> headwayCalculatorProvider = injector.getProvider(HeadwayCalculator.class);
		TransitSchedule schedule = injector.getInstance(TransitSchedule.class);
		Network network = injector.getInstance(Network.class);

		BatchPublicTransportRouter batchRouter = new BatchPublicTransportRouter(routerProvider,
				headwayCalculatorProvider, schedule, network, batchSize, numberOfThreads, interval);

		CsvMapper mapper = new CsvMapper();

		File inputFile = new File(cmd.getOptionStrict("input-path"));
		CsvSchema taskSchema = mapper.typedSchemaFor(Task.class).withHeader().withColumnSeparator(',').withComments()
				.withColumnReordering(true);

		MappingIterator<Task> taskIterator = mapper.readerWithTypedSchemaFor(Task.class).with(taskSchema)
				.readValues(inputFile);
		List<Task> tasks = taskIterator.readAll();

		Pair<Collection<TripInformation>, Collection<LegInformation>> results = batchRouter.run(tasks);
		Collection<TripInformation> tripResults = results.getLeft();
		Collection<LegInformation> legResults = results.getRight();

		if (outputTripsPath.isPresent()) {
			File outputFile = new File(outputTripsPath.get());
			CsvSchema resultSchema = mapper.typedSchemaFor(TripInformation.class).withHeader().withColumnSeparator(',');

			SequenceWriter writer = mapper.writerWithTypedSchemaFor(TripInformation.class).with(resultSchema)
					.writeValues(outputFile);
			writer.writeAll(tripResults);
		}

		if (outputLegsPath.isPresent()) {
			File outputFile = new File(outputLegsPath.get());
			CsvSchema resultSchema = mapper.typedSchemaFor(LegInformation.class).withHeader().withColumnSeparator(',');

			SequenceWriter writer = mapper.writerWithTypedSchemaFor(LegInformation.class).with(resultSchema)
					.writeValues(outputFile);
			writer.writeAll(legResults);
		}

		if (outputConfigPath.isPresent()) {
			new ConfigWriter(config).write(outputConfigPath.get());
		}
	}
}
