package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import com.fasterxml.jackson.core.JsonGenerator;
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
				.allowOptions("threads", "batch-size", "modes", "write-paths") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);

		boolean writePaths = cmd.getOption("write-paths").map(Boolean::parseBoolean).orElse(false);

		Set<String> modes = new HashSet<>();
		for (String mode : cmd.getOption("modes").orElse("car").split(",")) {
			modes.add(mode);
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new AbstractModule() {
					@Override
					public void install() {
					}

					@Provides
					@Singleton
					@Named("car")
					public Network provideCarNetwork(Network network) {
						Network carNetwork = NetworkUtils.createNetwork();
						new TransportModeNetworkFilter(network).filter(carNetwork, modes);
						new NetworkCleaner().run(carNetwork);
						return carNetwork;
					}
				}).build();

		Network network = injector.getInstance(Key.get(Network.class, Names.named("car")));

		BatchRoadRouter batchRouter = new BatchRoadRouter(injector.getProvider(LeastCostPathCalculatorFactory.class),
				network, batchSize, numberOfThreads, writePaths);

		CsvMapper taskMapper = new CsvMapper();

		File inputFile = new File(cmd.getOptionStrict("input-path"));
		CsvSchema taskSchema = taskMapper.typedSchemaFor(Task.class).withHeader().withColumnSeparator(',')
				.withComments().withColumnReordering(true);

		MappingIterator<Task> taskIterator = taskMapper.readerWithTypedSchemaFor(Task.class).with(taskSchema)
				.readValues(inputFile);
		List<Task> tasks = taskIterator.readAll();

		Collection<Result> results = batchRouter.run(tasks);

		CsvSchema.Builder builder = new CsvSchema.Builder() //
				.setColumnSeparator(',') //
				.setArrayElementSeparator(" ") //
				.setUseHeader(true) //
				.addColumn("identifier") //
				.addColumn("access_euclidean_distance_km") //
				.addColumn("egress_euclidean_distance_km") //
				.addColumn("in_vehicle_time_min") //
				.addColumn("in_vehicle_distance_km");

		if (writePaths) {
			builder.addArrayColumn("path");
		}

		CsvSchema schema = builder.build();

		File outputFile = new File(cmd.getOptionStrict("output-path"));

		CsvMapper resultMapper = new CsvMapper();
		resultMapper.configure(JsonGenerator.Feature.IGNORE_UNKNOWN, true);
		SequenceWriter writer = resultMapper.writerWithTypedSchemaFor(Result.class).with(schema)
				.writeValues(outputFile);

		writer.writeAll(results);
	}
}
