package org.eqasim.ile_de_france.scenario;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.ConfigAdapter;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

public class RunAdaptConfig {
	static public void main(String[] args) throws ConfigurationException {
		IDFConfigurator configurator = new IDFConfigurator();
		ConfigAdapter.run(args, configurator.getConfigGroups(), RunAdaptConfig::adaptConfiguration);
	}

	static public void adaptEstimators(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setCostModel(TransportMode.car, IDFModeChoiceModule.CAR_COST_MODEL_NAME);
		eqasimConfig.setCostModel(TransportMode.pt, IDFModeChoiceModule.PT_COST_MODEL_NAME);

		eqasimConfig.setEstimator(TransportMode.car, IDFModeChoiceModule.CAR_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.pt, IDFModeChoiceModule.PT_ESTIMATOR_NAME);
		eqasimConfig.setEstimator("car_passenger", IDFModeChoiceModule.PASSENGER_ESTIMATOR_NAME);
		eqasimConfig.setEstimator(TransportMode.bike, IDFModeChoiceModule.BIKE_ESTIMATOR_NAME);
	}

	static public void adaptConstraints(Config config) {
		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.remove(EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME);
		tripConstraints.add(IDFModeChoiceModule.INITIAL_WAITING_TIME_CONSTRAINT);
		dmcConfig.setTripConstraints(tripConstraints);
	}

	static public void adaptConfiguration(Config config) {
		adaptEstimators(config);
		adaptConstraints(config);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		dmcConfig.setModeAvailability(IDFModeChoiceModule.MODE_AVAILABILITY_NAME);

		// Potentially should be moved to the general GenerateConfig class. Wait time
		// should matter for routing!
		PlanCalcScoreConfigGroup scoringConfig = config.planCalcScore();
		scoringConfig.setMarginalUtlOfWaitingPt_utils_hr(-1.0);

		// Calibration results for 5%
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		if (eqasimConfig.getSampleSize() == 0.05) {
			// Adjust flow and storage capacity
			config.qsim().setFlowCapFactor(0.045);
			config.qsim().setStorageCapFactor(0.045);
		}
	}
}
