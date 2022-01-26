package org.eqasim.switzerland;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;

public class SwitzerlandConfigurator extends EqasimConfigurator {
	@Override
	public void adjustScenario(Scenario scenario) {
		super.adjustScenario(scenario);

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					copyAttribute(household, person, "spRegion");
					copyAttribute(household, person, "bikeAvailability");
				}
			}
		}
	}
}
