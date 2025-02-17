package org.eqasim.core.simulation.policies;

import org.eqasim.core.simulation.policies.config.PolicyConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

public class AttributePersonFilter implements PolicyPersonFilter {
	private final IdSet<Person> selection;
	private final boolean inverse;

	AttributePersonFilter(IdSet<Person> selection, boolean inverse) {
		this.selection = selection;
		this.inverse = inverse;
	}

	public boolean applies(Id<Person> personId) {
		if (selection == null) {
			return true; // no filtering
		} else if (!inverse) {
			return selection.contains(personId);
		} else {
			return !selection.contains(personId);
		}
	}

	static public PolicyPersonFilter create(Population population, PolicyConfigGroup policy) {
		if (policy.personFilter != null && policy.personFilter.length() > 0) {
			boolean inverse = false;
			String attribute = policy.personFilter;

			if (policy.personFilter.startsWith("!")) {
				inverse = true;
				attribute = policy.personFilter.substring(1);
			}

			IdSet<Person> selection = new IdSet<>(Person.class);

			for (Person person : population.getPersons().values()) {
				Boolean indicator = (Boolean) person.getAttributes().getAttribute(attribute);

				if (indicator != null && indicator) {
					selection.add(person.getId());
				}
			}

			return new AttributePersonFilter(selection, inverse);
		} else {
			return PolicyPersonFilter.NOOP;
		}
	}
}
