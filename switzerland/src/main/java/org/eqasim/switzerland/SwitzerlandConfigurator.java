package org.eqasim.switzerland;

import java.util.Random;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.households.Household;

public class SwitzerlandConfigurator extends EqasimConfigurator {
	static public void adjustScenario(Scenario scenario) {
		EqasimConfigurator.adjustScenario(scenario);
		Random random = new Random(0);

		for (Household household : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> memberId : household.getMemberIds()) {
				Person person = scenario.getPopulation().getPersons().get(memberId);

				if (person != null) {
					copyAttribute(household, person, "spRegion");

					String bikeAvailability = (String) household.getAttributes().getAttribute("bikeAvailability");

					if (!bikeAvailability.equals("FOR_NONE")) {
						person.getAttributes().putAttribute("bikeAvailability",
								random.nextDouble() < 0.2 ? "FOR_ALL" : "FOR_NONE");
					}

					copyAttribute(household, person, "bikeAvailability");
				}
			}
		}
	}
}
