package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;

public class IDFConfigurator extends EqasimConfigurator {
	public IDFConfigurator(CommandLine cmd) {
		super(cmd);

		registerModule(new IDFModeChoiceModule(cmd));
	}

	public void configureScenario(Scenario scenario) {
		super.configureScenario(scenario);

		Config config = scenario.getConfig();

		config.routing().getTeleportedModeParams().get("walk").setBeelineDistanceFactor(1.3);
		config.routing().getTeleportedModeParams().get("walk").setTeleportedModeSpeed(3.25 / 3.6);
		
		config.routing().getTeleportedModeParams().get("bicycle").setBeelineDistanceFactor(1.3);
		config.routing().getTeleportedModeParams().get("bicycle").setTeleportedModeSpeed(9.1 / 3.6);
	}
}
