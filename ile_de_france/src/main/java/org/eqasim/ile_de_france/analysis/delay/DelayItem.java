package org.eqasim.ile_de_france.analysis.delay;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

class DelayItem {
	public Id<Person> personId;
	public int activityIndex;

	public double plannedStartTime;
	public double plannedEndTime;

	public double simulatedStartTime;
	public double simulatedEndTime;
}
