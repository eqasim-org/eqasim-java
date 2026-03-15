package org.eqasim.ile_de_france.vdf;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFConfigGroup.HandlerType;
import org.eqasim.core.simulation.vdf.VDFModule;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunVolumeDelay {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("demand-path", "network-path", "output-path", "horizon", "travel-time-interval",
						"replanning-rate", "use-engine", "generate-network-events", "iterations", "sampling-rate",
						"seed", "threads", "capacity-factor") //
				.build();

		String demandPath = cmd.getOptionStrict("demand-path");
		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");
		int horizon = Integer.parseInt(cmd.getOptionStrict("horizon"));
		double travelTimeInterval = Double.parseDouble(cmd.getOptionStrict("travel-time-interval"));
		double replanningRate = Double.parseDouble(cmd.getOptionStrict("replanning-rate"));
		boolean useEngine = Boolean.parseBoolean(cmd.getOptionStrict("use-engine"));
		boolean generateNetworkEvents = Boolean.parseBoolean(cmd.getOptionStrict("generate-network-events"));
		int iterations = Integer.parseInt(cmd.getOptionStrict("iterations"));
		double samplingRate = Double.parseDouble(cmd.getOptionStrict("sampling-rate"));
		int seed = Integer.parseInt(cmd.getOptionStrict("seed"));
		int threads = Integer.parseInt(cmd.getOptionStrict("threads"));
		double capacityFactor = Double.parseDouble(cmd.getOptionStrict("capacity-factor"));

		Config config = ConfigUtils.createConfig();

		config.global().setRandomSeed(seed);
		config.global().setNumberOfThreads(threads);

		config.controller().setOutputDirectory(outputPath);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(iterations);

		VDFConfigGroup vdfConfig = new VDFConfigGroup();
		config.addModule(vdfConfig);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		vdfConfig.setHorizon(horizon);
		vdfConfig.setInterval(travelTimeInterval);
		vdfConfig.setHandler(HandlerType.SparseHorizon);
		vdfConfig.setCapacityFactor(capacityFactor);

		EqasimConfigGroup eqasimConfig = new EqasimConfigGroup();
		eqasimConfig.setSampleSize(samplingRate);
		config.addModule(eqasimConfig);

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
		EqasimFixes.install(controller);

		controller.addOverridingModule(new VDFModule());

		if (useEngine) {
			VDFEngineConfigGroup engineConfig = new VDFEngineConfigGroup();
			config.addModule(engineConfig);

			engineConfig.setGenerateNetworkEventsInterval(generateNetworkEvents ? 1 : 0);

			controller.addOverridingModule(new VDFEngineModule());
			controller.configureQSimComponents(VDFEngineModule::configureQSim);
		}

		controller.run();
	}
}