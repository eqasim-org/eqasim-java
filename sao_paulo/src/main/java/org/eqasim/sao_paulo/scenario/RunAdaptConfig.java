package org.eqasim.sao_paulo.scenario;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.sao_paulo.mode_choice.SaoPauloModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

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

		eqasimConfig.setCostModel(TransportMode.car, SaoPauloModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, SaoPauloModeChoiceModule.PT_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.taxi, SaoPauloModeChoiceModule.TAXI_COST_MODEL_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(SaoPauloModeChoiceModule.MODE_AVAILABILITY_NAME);
		Collection<String> tripConstraints = dmcConfig.getTripConstraints();
		tripConstraints.add("WalkDurationConstraint");
		dmcConfig.setTripConstraints(tripConstraints);
		
		List<String> networkModes = new LinkedList<>(config.plansCalcRoute().getNetworkModes());
		networkModes.add("taxi");
		config.plansCalcRoute().setNetworkModes(networkModes);
		
		ModeParams taxiParams = new ModeParams("taxi");
		config.planCalcScore().addModeParams(taxiParams);
		
	}
}
