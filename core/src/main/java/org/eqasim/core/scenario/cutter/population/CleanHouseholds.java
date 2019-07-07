package org.eqasim.core.scenario.cutter.population;

import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.households.Household;
import org.matsim.households.Households;

public class CleanHouseholds {
	final private Collection<Id<Person>> personsIds;

	public CleanHouseholds(Population population) {
		this.personsIds = population.getPersons().values().stream().map(Person::getId).collect(Collectors.toSet());
	}

	public void run(Households households) {
		Iterator<Household> iterator = households.getHouseholds().values().iterator();

		while (iterator.hasNext()) {
			Household household = iterator.next();
			Iterator<Id<Person>> memberIterator = household.getMemberIds().iterator();

			while (memberIterator.hasNext()) {
				Id<Person> memberId = memberIterator.next();

				if (!personsIds.contains(memberId)) {
					memberIterator.remove();
				}
			}

			if (household.getMemberIds().size() == 0) {
				iterator.remove();
			}
		}
	}
}
