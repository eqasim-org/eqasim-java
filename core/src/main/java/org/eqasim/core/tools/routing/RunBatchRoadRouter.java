package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.tools.routing.BatchRoadRouter.Result;
import org.eqasim.core.tools.routing.BatchRoadRouter.Task;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class RunBatchRoadRouter {
	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				EqasimConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		EqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(EqasimConfigurator.getModules()) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
					}

					@Provides
					@Singleton
					@Named("car")
					public Network provideCarNetwork(Network network) {
						Network carNetwork = NetworkUtils.createNetwork();
						new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));
						new NetworkCleaner().run(carNetwork);
						return carNetwork;
					}
				}).build();

		Network network = injector.getInstance(Key.get(Network.class, Names.named("car")));

		BatchRoadRouter batchRouter = new BatchRoadRouter(injector.getProvider(LeastCostPathCalculatorFactory.class),
				network, batchSize, numberOfThreads);

		CsvMapper mapper = new CsvMapper();

		File inputFile = new File(cmd.getOptionStrict("input-path"));
		CsvSchema taskSchema = mapper.typedSchemaFor(Task.class).withHeader().withColumnSeparator(',').withComments()
				.withColumnReordering(true);

		MappingIterator<Task> taskIterator = mapper.readerWithTypedSchemaFor(Task.class).with(taskSchema)
				.readValues(inputFile);
		List<Task> tasks = taskIterator.readAll();

		Collection<Result> results = batchRouter.run(tasks);

		File outputFile = new File(cmd.getOptionStrict("output-path"));
		CsvSchema resultSchema = mapper.typedSchemaFor(Result.class).withHeader().withColumnSeparator(',');

		SequenceWriter writer = mapper.writerWithTypedSchemaFor(Result.class).with(resultSchema)
				.writeValues(outputFile);
		writer.writeAll(results);
	}
}
