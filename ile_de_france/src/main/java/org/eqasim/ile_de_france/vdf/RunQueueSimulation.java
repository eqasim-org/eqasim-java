package org.eqasim.ile_de_france.vdf;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunQueueSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("demand-path", "network-path", "flow-capacity-factor", "storage-capacity-factor",
						"replanning-rate", "travel-time-interval", "output-path", "iterations", "seed", "threads") //
				.build();

		// TODO: travel time getter?
		// TODO: travel time aggregator?
		// TODO: traffic dynamics?
		// TODO: stuck dynamics?

		// TODO: base everything on default travel time interval? what is it?
		// TODO: What replanning rate to choose for stability?

		String demandPath = cmd.getOptionStrict("demand-path");
		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");
		double flowCapacityFactor = Double.parseDouble(cmd.getOptionStrict("flow-capacity-factor"));
		double storageCapacityFactor = Double.parseDouble(cmd.getOptionStrict("storage-capacity-factor"));
		double replanningRate = Double.parseDouble(cmd.getOptionStrict("replanning-rate"));
		double travelTimeInterval = Double.parseDouble(cmd.getOptionStrict("travel-time-interval"));
		int iterations = Integer.parseInt(cmd.getOptionStrict("iterations"));
		int seed = Integer.parseInt(cmd.getOptionStrict("seed"));
		int threads = Integer.parseInt(cmd.getOptionStrict("threads"));
		
		Config config = ConfigUtils.createConfig();

		config.global().setRandomSeed(seed);
		config.global().setNumberOfThreads(threads);

		config.controller().setOutputDirectory(outputPath);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(iterations);

		config.travelTimeCalculator().setTraveltimeBinSize(travelTimeInterval);
		config.travelTimeCalculator().setTravelTimeAggregatorType("optimistic");
		config.travelTimeCalculator().setTravelTimeGetterType("average");

		config.qsim().setTrafficDynamics(TrafficDynamics.queue);
		config.qsim().setFlowCapFactor(flowCapacityFactor);
		config.qsim().setStorageCapFactor(storageCapacityFactor);

		StrategySettings rerouteStrategy = new StrategySettings();
		rerouteStrategy.setStrategyName("ReRoute");
		rerouteStrategy.setWeight(replanningRate);
		config.replanning().addStrategySettings(rerouteStrategy);

		StrategySettings keepSelectedStrategy = new StrategySettings();
		keepSelectedStrategy.setStrategyName("KeepLastSelected");
		keepSelectedStrategy.setWeight(1.0 - replanningRate);
		config.replanning().addStrategySettings(keepSelectedStrategy);

		config.replanning().setMaxAgentPlanMemorySize(1);

		ActivityParams activityParams = new ActivityParams("generic");
		activityParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(activityParams);

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		new PopulationReader(scenario).readFile(demandPath);

		Controler controller = new Controler(scenario);
		TravelTimeTracker.install(controller);
		NonSelectedPlanSelector.install(controller);
		controller.run();
	}
}
