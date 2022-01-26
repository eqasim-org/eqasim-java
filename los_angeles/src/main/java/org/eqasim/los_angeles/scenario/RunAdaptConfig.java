package org.eqasim.los_angeles.scenario;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.los_angeles.mode_choice.LosAngelesModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.FallbackBehaviour;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;

public class RunAdaptConfig {
	protected final static List<String> ACTIVITY_TYPES = Arrays.asList("business");

	static public void main(String[] args) throws ConfigurationException {
		EqasimConfigurator configurator = new EqasimConfigurator();
		ConfigAdapter.run(args, configurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptConfiguration(Config config) {
		// Ignore some input files
		config.transit().setVehiclesFile(null);
		config.households().setInputFile(null);

		// Set up mode choice
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, LosAngelesModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, LosAngelesModeChoiceModule.PT_COST_MODEL_NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		Collection<String> tripConstraints = dmcConfig.getTripConstraints();
		tripConstraints.add("WalkDurationConstraint");
		dmcConfig.setTripConstraints(tripConstraints);
		dmcConfig.setModeAvailability(LosAngelesModeChoiceModule.MODE_AVAILABILITY_NAME);
		dmcConfig.setTourConstraintsAsString("FromTripBased, VehicleTourConstraintWithCarPassenger");
		dmcConfig.setFallbackBehaviour(FallbackBehaviour.INITIAL_CHOICE);
		PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();

		for (String activityType : ACTIVITY_TYPES) {
			ActivityParams activityParams = scoringConfig.getActivityParams(activityType);

			if (activityParams == null) {
				activityParams = new ActivityParams(activityType);
				config.planCalcScore().addActivityParams(activityParams);
			}

			activityParams.setScoringThisActivityAtAll(false);
		}
	}
}
