package org.eqasim.core.simulation.policies;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public interface PolicyPersonFilter {
	static public PolicyPersonFilter NOOP = personId -> true;

	public boolean applies(Id<Person> personId);
}
