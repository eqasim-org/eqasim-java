package org.eqasim.ile_de_france.preparation.config;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.config.GenerateConfig;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class GenerateIDFConfig extends GenerateConfig {
	public GenerateIDFConfig(CommandLine cmd, String prefix, double sampleSize, int randomSeed, int threads) {
		super(cmd, prefix, sampleSize, randomSeed, threads);
	}

	@Override
	protected void applyModifications(Config config) {
		super.applyModifications(config);

		// Discrete mode choice
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(IDFModeChoiceModule.MODE_AVAILABILITY_NAME);

		// Update utility bindings
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);

		eqasimConfig.addCostModelMapping("car", "idf_car");
		eqasimConfig.addCostModelMapping("pt", "idf_pt");
	}
}
