package org.eqasim.projects.dynamic_av;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.households.Household;

import ch.ethz.matsim.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;

public class DAConfigurator extends EqasimConfigurator {
	private DAConfigurator() {
	}

	static public void configure(Config config) {
		EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);

		eqasimConfig.setEstimator(TransportMode.car, DAModule.DA_CAR_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.pt, DAModule.DA_PT_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.bike, DAModule.DA_BIKE_ESTIMATOR);
		eqasimConfig.setEstimator(TransportMode.walk, DAModule.DA_WALK_ESTIMATOR);

		DiscreteModeChoiceConfigGroup dmcConfig = (DiscreteModeChoiceConfigGroup) config.getModules()
				.get(DiscreteModeChoiceConfigGroup.GROUP_NAME);
		dmcConfig.setModeAvailability(DAModule.DA_AV_MODE_AVAILABILITY_NAME);
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
