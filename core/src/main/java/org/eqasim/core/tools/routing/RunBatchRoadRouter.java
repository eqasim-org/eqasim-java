package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eqasim.core.tools.routing.BatchRoadRouter.Result;
import org.eqasim.core.tools.routing.BatchRoadRouter.Task;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.speedy.SpeedyALTFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class RunBatchRoadRouter {
	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"));
		new NetworkCleaner().run(roadNetwork);

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);

		BatchRoadRouter batchRouter = new BatchRoadRouter(new SpeedyALTFactory(), roadNetwork, batchSize, numberOfThreads);

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
