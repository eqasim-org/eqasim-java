package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class IDFConfigurator extends EqasimConfigurator {
	public IDFConfigurator(CommandLine cmd) {
		super(cmd);

		registerModule(new IDFModeChoiceModule(cmd));
	}
}
