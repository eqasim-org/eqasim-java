package org.eqasim.ile_de_france.vdf;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.InflowCapacitySetting;
import org.matsim.core.config.groups.QSimConfigGroup.NodeTransition;
import org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunQueueSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("demand-path", "network-path", "flow-capacity-factor", "storage-capacity-factor",
						"replanning-rate", "interval", "getter", "aggregator") //
				.build();

		String demandPath = cmd.getOptionStrict("demand-path");
		String networkPath = cmd.getOptionStrict("network-path");
		double flowCapacityFactor = Double.parseDouble(cmd.getOptionStrict("flow-capacity-factor"));
		double storageCapacityFactor = Double.parseDouble(cmd.getOptionStrict("storage-capacity-factor"));
		double replanningRate = Double.parseDouble(cmd.getOptionStrict("replanning-rate"));
		double interval = Double.parseDouble(cmd.getOptionStrict("interval"));
		TrafficDynamics trafficDynamics = TrafficDynamics.valueOf(cmd.getOptionStrict("traffic-dynamics"));
		
		Config config = ConfigUtils.createConfig();

		config.travelTimeCalculator().setTraveltimeBinSize(interval);

		config.qsim().setTrafficDynamics(trafficDynamics);
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
		config.replanning().setPlanSelectorForRemoval("UnselectedRemoval");

		Scenario scenario = ScenarioUtils.createScenario(config);
		new MatsimNetworkReader(scenario.getNetwork()).readFile(networkPath);
		new PopulationReader(scenario).readFile(demandPath);

		Controler controller = new Controler(scenario);
		TravelTimeTracker.install(controller);
		controller.run();
	}
}