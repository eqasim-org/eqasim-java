package org.eqasim.wayne_county;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunStandardMATSimSimulation {

	public static void main(String[] args) {

		Config config = ConfigUtils.loadConfig(args[0]);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
		ScenarioUtils.loadScenario(scenario);
		Controler controller = new Controler(scenario);
		controller.run();
	}

}
