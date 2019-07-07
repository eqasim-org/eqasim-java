package org.eqasim.core.scenario.cutter.population;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;

public class RemoveEmptyPlans {
	public void run(Population population) {
		Iterator<? extends Person> personIterator = population.getPersons().values().iterator();

		while (personIterator.hasNext()) {
			Person person = personIterator.next();

			List<Plan> plansToRemove = person.getPlans().stream().filter(p -> p.getPlanElements().size() == 0)
					.collect(Collectors.toList());
			plansToRemove.forEach(p -> person.removePlan(p));

			if (person.getPlans().size() == 0) {
				personIterator.remove();
			}
		}
	}
}
