package org.eqasim.switzerland.scenario;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.switzerland.SwitzerlandConfigurator;
import org.eqasim.switzerland.mode_choice.SwissModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		ConfigAdapter.run(args, SwitzerlandConfigurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, SwissModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, SwissModeChoiceModule.BIKE_ESTIMATOR_NAME);

		eqasimConfig.setCostModel(TransportMode.car, SwissModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SwissModeChoiceModule.PT_COST_MODEL_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SwissModeChoiceModule.MODE_AVAILABILITY_NAME);
	}
}
