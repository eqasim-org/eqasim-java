package org.eqasim.automated_vehicles.components;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class AvPersonAnalysisFilter implements PersonAnalysisFilter {
	@Override
	public boolean analyzePerson(Id<Person> personId) {
		if (personId.toString().startsWith("av")) {
			return false;
		}

		return true;
	}
}
