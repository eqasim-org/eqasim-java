package org.eqasim.ile_de_france.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.components.headway.HeadwayImputerModule;
import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.routing.IDFRaptorModule;
import org.eqasim.ile_de_france.tools.routing.BatchPublicTransportRouter.LegInformation;
import org.eqasim.ile_de_france.tools.routing.BatchPublicTransportRouter.Task;
import org.eqasim.ile_de_france.tools.routing.BatchPublicTransportRouter.TripInformation;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup.ModeRoutingParams;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
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

public class RunBatchPublicTransportRouter {
	private final static Logger logger = Logger.getLogger(RunBatchPublicTransportRouter.class);

	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path") //
				.allowOptions("threads", "batch-size", "interval", //
						"transfer-utility", "waiting-utility", //
						"direct-walk-factor", "maximum-transfer-distance", //
						"walk-factor", "walk-speed", //
						"output-trips-path", "output-legs-path", "output-config-path") //
				.allowPrefixes("raptor") //
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

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		double interval = (double) cmd.getOption("interval").map(Integer::parseInt).orElse(0);

		Optional<String> outputLegsPath = cmd.getOption("output-legs-path");
		Optional<String> outputTripsPath = cmd.getOption("output-trips-path");
		Optional<String> outputConfigPath = cmd.getOption("output-config-path");

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new IDFRaptorModule(cmd)) //
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
