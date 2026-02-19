package org.eqasim.ile_de_france.vdf;

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
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunVolumeDelay {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("demand-path", "network-path", "handler", "horizon", "interval",
						"replanning-rate", "use-engine", "generate-network-events") //
				.build();

		String demandPath = cmd.getOptionStrict("demand-path");
		String networkPath = cmd.getOptionStrict("network-path");
		int horizon = Integer.parseInt(cmd.getOptionStrict("horizon"));
		double interval = Double.parseDouble(cmd.getOptionStrict("interval"));
		double replanningRate = Double.parseDouble(cmd.getOptionStrict("replanning-rate"));
		HandlerType handler = HandlerType.valueOf(cmd.getOptionStrict("handler"));
		boolean useEngine = Boolean.parseBoolean(cmd.getOptionStrict("use-engine"));
		boolean generateNetworkEvents = Boolean.parseBoolean(cmd.getOptionStrict("generate-network-events"));

		Config config = ConfigUtils.createConfig();

		VDFConfigGroup vdfConfig = new VDFConfigGroup();
		config.addModule(vdfConfig);

		config.qsim().setFlowCapFactor(1e9);
		config.qsim().setStorageCapFactor(1e9);

		vdfConfig.setHorizon(horizon);
		vdfConfig.setInterval(interval);
		vdfConfig.setHandler(handler);

		StrategySettings rerouteStrategy = new StrategySettings();
		rerouteStrategy.setStrategyName("ReRoute");
		rerouteStrategy.setWeight(replanningRate);
		config.replanning().addStrategySettings(rerouteStrategy);

		StrategySettings keepSelectedStrategy = new StrategySettings();
		keepSelectedStrategy.setStrategyName("KeepLastSelected");
		keepSelectedStrategy.setWeight(1.0 - replanningRate);
		config.replanning().addStrategySettings(keepSelectedStrategy);

		config.replanning().setMaxAgentPlanMemorySize(1);
		config.replanning().setPlanSelectorForRemoval("UnselectedRemoval");

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		new PopulationReader(scenario).readFile(demandPath);

		Controler controller = new Controler(scenario);

		controller.addOverridingModule(new VDFModule());

		if (useEngine) {
			VDFEngineConfigGroup engineConfig = new VDFEngineConfigGroup();
			config.addModule(engineConfig);

			engineConfig.setGenerateNetworkEvents(generateNetworkEvents);

			controller.addOverridingModule(new VDFEngineModule());
		}

		TravelTimeTracker.install(controller);
		controller.run();
	}
}