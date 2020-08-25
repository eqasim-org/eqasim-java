package org.eqasim.ile_de_france;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class IDFConfigurator extends EqasimConfigurator {
	static public void configure(Config config) {
		for (ModeParams mode : config.planCalcScore().getModes().values()) {
			mode.setMarginalUtilityOfDistance(0.0);
			mode.setMarginalUtilityOfTraveling(-1.0);
			mode.setConstant(0.0);
			mode.setDailyMonetaryConstant(0.0);
			mode.setDailyUtilityConstant(0.0);
			mode.setMonetaryDistanceRate(0.0);
		}

		config.planCalcScore().setMarginalUtilityOfMoney(0.0);
		config.planCalcScore().setPerforming_utils_hr(0.0);
		config.planCalcScore().setMarginalUtlOfWaiting_utils_hr(-1.0);
		config.planCalcScore().setMarginalUtlOfWaitingPt_utils_hr(-1.0);
		config.planCalcScore().setUtilityOfLineSwitch(-0.1);

		DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.remove(EqasimModeChoiceModule.PASSENGER_CONSTRAINT_NAME);
		dmcConfig.setTripConstraints(tripConstraints);

		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
		eqasimConfig.setEstimator("car_passenger", IDFModeChoiceModule.PASSENGER_ESTIMATOR_NAME);
	}
}
