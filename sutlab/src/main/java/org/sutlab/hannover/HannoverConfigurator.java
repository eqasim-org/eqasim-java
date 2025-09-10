package org.sutlab.hannover;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.sutlab.hannover.mode_choice.HannoverModeChoiceModule;
import org.matsim.core.config.CommandLine;

public class HannoverConfigurator extends EqasimConfigurator {
	public HannoverConfigurator(CommandLine cmd) {
		super(cmd);

		registerModule(new HannoverModeChoiceModule(cmd));
	}
}