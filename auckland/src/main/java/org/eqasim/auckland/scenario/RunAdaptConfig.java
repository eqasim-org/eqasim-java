package org.eqasim.auckland.scenario;

import org.eqasim.auckland.costs.AucklandCarCostModel;
import org.eqasim.auckland.costs.AucklandPtCostModel;
import org.eqasim.auckland.mode_choice.AucklandModeAvailability;
import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		ConfigAdapter.run(args, EqasimConfigurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		// Ignore some input files
		config.transit().setVehiclesFile(null);
		config.households().setInputFile(null);

		// Set up mode choice
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, AucklandCarCostModel.NAME);
		eqasimConfig.setCostModel(TransportMode.pt, AucklandPtCostModel.NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(AucklandModeAvailability.NAME);
	}
}
