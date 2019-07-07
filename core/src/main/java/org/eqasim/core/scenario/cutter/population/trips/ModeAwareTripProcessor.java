package org.eqasim.core.scenario.cutter.population.trips;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

public class ModeAwareTripProcessor implements TripProcessor {
	private final MainModeIdentifier mainModeIdentifier;
	private final Map<String, TripProcessor> processors = new HashMap<>();

	public ModeAwareTripProcessor(MainModeIdentifier mainModeIdentifier) {
		this.mainModeIdentifier = mainModeIdentifier;
	}

	public void setProcessor(String mode, TripProcessor processor) {
		this.processors.put(mode, processor);
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		String mainMode = mainModeIdentifier.identifyMainMode(trip);
		return processors.get(mainMode).process(firstActivity, trip, secondActivity);
	}
}
