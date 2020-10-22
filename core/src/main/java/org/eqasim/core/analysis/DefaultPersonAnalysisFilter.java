package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DefaultPersonAnalysisFilter implements PersonAnalysisFilter {
	@Override
	public boolean analyzePerson(Id<Person> personId) {
		return !personId.toString().startsWith("pt_");
	}
}
