package org.eqasim.core.scenario.cutter.population.trips;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class ModeAwareTripProcessor implements TripProcessor {
	private final Map<String, TripProcessor> processors = new HashMap<>();

	public void setProcessor(String mode, TripProcessor processor) {
		this.processors.put(mode, processor);
	}

	@Override
	public List<PlanElement> process(Id<Person> personId, int firstLegIndex, Activity firstActivity,
			List<PlanElement> trip, Activity secondActivity, String routingMode) {
		return processors.get(routingMode).process(personId, firstLegIndex, firstActivity, trip, secondActivity,
				routingMode);
	}
}
