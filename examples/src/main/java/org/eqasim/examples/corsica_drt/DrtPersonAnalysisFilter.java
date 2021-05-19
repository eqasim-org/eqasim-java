package org.eqasim.examples.corsica_drt;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class DrtPersonAnalysisFilter implements PersonAnalysisFilter {
	@Override
	public boolean analyzePerson(Id<Person> personId) {
		if (personId.toString().startsWith("drt")) {
			return false;
		}

		return true;
	}
}
