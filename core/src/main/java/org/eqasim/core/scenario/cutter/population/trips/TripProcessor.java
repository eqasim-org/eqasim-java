package org.eqasim.core.scenario.cutter.population.trips;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public interface TripProcessor {
	List<PlanElement> process(Person person, int tripIndex, Activity firstActivity, List<PlanElement> trip,
			Activity secondActivity);
}
