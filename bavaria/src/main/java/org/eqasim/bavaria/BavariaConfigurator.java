package org.eqasim.bavaria;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.bavaria.mode_choice.BavariaModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class BavariaConfigurator extends EqasimConfigurator {
	public BavariaConfigurator(CommandLine cmd) {
		super(cmd);

		registerModule(new BavariaModeChoiceModule(cmd));
	}
}
