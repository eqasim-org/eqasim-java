package org.eqasim.core.tools.routing;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.eqasim.core.components.headway.HeadwayCalculator;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.Result;
import org.eqasim.core.tools.routing.BatchPublicTransportRouter.Task;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class RunBatchPublicTransportRouter {
	static public void main(String[] args) throws ConfigurationException, JsonGenerationException, JsonMappingException,
			IOException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "network-path", "input-path", "output-path") //
				.allowOptions("threads", "batch-size", "interval", "transfer-utility") //
				.build();

		Config config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup());

		// Make sure we find shortest travel time path

		ModeParams modeParams = config.planCalcScore().getOrCreateModeParams("pt");
		modeParams.setConstant(0.0);
		modeParams.setDailyMonetaryConstant(0.0);
		modeParams.setDailyUtilityConstant(0.0);
		modeParams.setMarginalUtilityOfDistance(0.0);
		modeParams.setMarginalUtilityOfTraveling(-1.0);
		modeParams.setMonetaryDistanceRate(0.0);

		config.planCalcScore().setMarginalUtilityOfMoney(0.0);
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(0.0);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-1.0);

		if (cmd.hasOption("transfer-utility")) {
			config.planCalcScore().setUtilityOfLineSwitch(Double.parseDouble(cmd.getOptionStrict("transfer-utility")));
		}

		// Load data

		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
		new MatsimNetworkReader(scenario.getNetwork()).readFile(cmd.getOptionStrict("network-path"));

		// Additiona settings

		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());
		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		double interval = (double) cmd.getOption("interval").map(Integer::parseInt).orElse(0);

		// Set up builders

		RaptorStaticConfig raptorConfig = RaptorUtils.createStaticConfig(config);
		SwissRailRaptorData raptorData = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, raptorConfig,
				scenario.getNetwork(), null);

		SwissRailRaptor.Builder routerBuilder = new SwissRailRaptor.Builder(raptorData, config);
		HeadwayCalculator.Builder headwayBuilder = new HeadwayCalculator.Builder().withInterval(interval);

		BatchPublicTransportRouter batchRouter = new BatchPublicTransportRouter(routerBuilder, headwayBuilder,
				scenario.getTransitSchedule(), scenario.getNetwork(), batchSize, numberOfThreads, interval);

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
