package org.eqasim.cairo.scenario;

import java.util.Arrays;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		IDFConfigurator configurator = new IDFConfigurator();
		ConfigAdapter.run(args, configurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		org.eqasim.ile_de_france.scenario.RunAdaptConfig.adaptConfiguration(config);

		for (String activityType : Arrays.asList("work", "personal", "primary", "secondary", "uni", "shopping")) {
			ActivityParams activityParams = new ActivityParams(activityType);
			activityParams.setScoringThisActivityAtAll(false);
			config.planCalcScore().addActivityParams(activityParams);
		}
	}
}
