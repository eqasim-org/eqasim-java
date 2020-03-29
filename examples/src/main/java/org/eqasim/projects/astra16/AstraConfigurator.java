package org.eqasim.projects.astra16;

import java.util.HashSet;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.calibration.CalibrationConfigGroup;
import org.eqasim.projects.astra16.mode_choice.InfiniteHeadwayConstraint;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraBikeUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraCarUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraPtUtilityEstimator;
import org.eqasim.projects.astra16.mode_choice.estimators.AstraWalkUtilityEstimator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.households.Household;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

public class AstraConfigurator extends EqasimConfigurator {
	private AstraConfigurator() {
	}

	static public ConfigGroup[] getConfigGroups() {
		return new ConfigGroup[] { //
				new SwissRailRaptorConfigGroup(), //
				new EqasimConfigGroup(), //
				new DiscreteModeChoiceConfigGroup(), //
				new CalibrationConfigGroup(), //
		};
	}

	static public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, AstraCarUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.pt, AstraPtUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.bike, AstraBikeUtilityEstimator.NAME);
		eqasimConfig.setEstimator(TransportMode.walk, AstraWalkUtilityEstimator.NAME);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);

		Set<String> tripConstraints = new HashSet<>(dmcConfig.getTripConstraints());
		tripConstraints.add(InfiniteHeadwayConstraint.NAME);
		dmcConfig.setTripConstraints(tripConstraints);
	}

	static public void adjustScenario(Scenario scenario) {
		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					person.getAttributes().putAttribute("householdIncome", household.getIncome().getIncome());
				}
			}
		}
	}
}
