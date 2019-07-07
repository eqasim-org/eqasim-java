package org.eqasim.core.scenario.cutter.population.trips;

import java.util.List;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;

public interface TripProcessor {
	List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity);
}
