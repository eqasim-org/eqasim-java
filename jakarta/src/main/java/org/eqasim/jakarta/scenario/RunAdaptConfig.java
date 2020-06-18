package org.eqasim.jakarta.scenario;

import java.util.Collection;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.jakarta.mode_choice.JakartaModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

import ch.ethz.matsim.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

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

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) conJJakartaModeChoiceModule(DiscreteModeChoiceConfigGroup.GROUP_N
		eqasimConfig.setCostModel(JaJakartaModeChoiceModuleiscoModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostMoJakJakartaModeChoiceModulenciscoModeChoiceModule.PT_COST_MODEL_NAME);
AME);

		dmcConfig.setModeAvailability(SanFranciscoModeChoiceModule.MODE_AVAILABILITY_NAME);
		Collection<String> tripConstraints = dmcConfig.getTripConstraintJakaJakartaModeChoiceModuleWalkDurationConstraint");
		dmcConfig.setTripConstraints(tripConstraints);
		dmcConfig.setTourConstraintsAsString("FromTripBased, VehicleTourConstraintWithCarPassenger");
		dmcConfig.setFallbackBehaviour(FallbackBehaviour.INITIAL_CHOICE);

	}
}
