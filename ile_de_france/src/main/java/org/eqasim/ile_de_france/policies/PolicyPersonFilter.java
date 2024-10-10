package org.eqasim.ile_de_france.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class PolicyPersonFilter {
	private final IdSet<Person> selection;

	PolicyPersonFilter(IdSet<Person> selection) {
		this.selection = selection;
	}

	public boolean applies(Id<Person> personId) {
		return selection == null ? true : selection.contains(personId);
	}

	static public PolicyPersonFilter create(Population population, PolicyConfigGroup policy) {
		if (policy.personFilter != null && policy.personFilter.length() > 0) {
			IdSet<Person> selection = new IdSet<>(Person.class);

			for (Person person : population.getPersons().values()) {
				Boolean indicator = (Boolean) person.getAttributes().getAttribute(policy.personFilter);

				if (indicator != null && indicator) {
					selection.add(person.getId());
				}
			}

			return new PolicyPersonFilter(selection);
		} else {
			return new PolicyPersonFilter(null);
		}
	}
}
