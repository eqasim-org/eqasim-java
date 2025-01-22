package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.policies.PoliciesConfigGroup;
import org.matsim.core.config.CommandLine;

public class IDFConfigurator extends EqasimConfigurator {
	public IDFConfigurator(CommandLine cmd) {
		super(cmd);

		registerConfigGroup(new PoliciesConfigGroup(), true);

		registerModule(new IDFModeChoiceModule(cmd));
	}
}
