package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
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
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Injector;
import com.google.inject.Provider;

public class RunBatchPublicTransportRouter {
	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size", "interval", "transfer-utility", "output-legs-path") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		if (cmd.hasOption("transfer-utility")) {
			config.planCalcScore().setUtilityOfLineSwitch(Double.parseDouble(cmd.getOptionStrict("transfer-utility")));
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		double interval = (double) cmd.getOption("interval").map(Integer::parseInt).orElse(0);
		Optional<String> outputLegsPath = cmd.getOption("output-legs-path");

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

		{
			File outputFile = new File(cmd.getOptionStrict("output-path"));
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
	}
}
