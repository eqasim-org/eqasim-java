package org.eqasim.switzerland.scenario.config;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.config.GenerateConfig;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class GenerateSwitzerlandConfig extends GenerateConfig {
	public GenerateSwitzerlandConfig(CommandLine cmd, String prefix, double sampleSize, int randomSeed, int threads) {
		super(cmd, prefix, sampleSize, randomSeed, threads);
	}

	@Override
	protected void applyModifications(Config config) {
		super.applyModifications(config);

		// Discrete mode choice
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SwissModeChoiceModule.MODE_AVAILABILITY_NAME);

		// Update utility bindings
		EqasimConfigGroup eqasimConfig = (EqasimConfigGroup) config.getModules().get(EqasimConfigGroup.GROUP_NAME);

		eqasimConfig.removeModeUtilityMapping("car");
		eqasimConfig.removeModeUtilityMapping("bike");

		eqasimConfig.addModeUtilityMapping("car", "swiss_car");
		eqasimConfig.addModeUtilityMapping("bike", "swiss_bike");

		eqasimConfig.addCostModelMapping("car", "swiss_car");
		eqasimConfig.addCostModelMapping("pt", "swiss_pt");
	}
}
