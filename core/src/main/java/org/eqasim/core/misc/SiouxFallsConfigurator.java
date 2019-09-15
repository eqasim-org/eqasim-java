package org.eqasim.core.misc;

import java.net.URL;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;

import ch.ethz.matsim.discrete_mode_choice.modules.DiscreteModeChoiceConfigurator;

public class SiouxFallsConfigurator {
	static public Config configure(URL configUrl, ConfigGroup... configGroups) {
		Config config = ConfigUtils.loadConfig(configUrl, configGroups);

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		DiscreteModeChoiceConfigurator.configureAsModeChoiceInTheLoop(config);

		ModeParams bikeParams = new ModeParams("bike");
		config.planCalcScore().addModeParams(bikeParams);

		return config;
	}
}
