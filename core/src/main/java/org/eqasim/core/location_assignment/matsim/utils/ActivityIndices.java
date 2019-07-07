package org.eqasim.core.location_assignment.matsim.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;

public class ActivityIndices {
	final private Optional<Integer> originActivityIndex;
	final private Optional<Integer> destinationActivityIndex;
	final private List<Integer> chainIndices;

	public ActivityIndices(Optional<Integer> originActivityIndex, Optional<Integer> destinationActivityIndex,
			List<Integer> chainIndices) {
		this.originActivityIndex = originActivityIndex;
		this.destinationActivityIndex = destinationActivityIndex;
		this.chainIndices = chainIndices;
	}

	public Optional<Integer> getOriginActivityIndex() {
		return originActivityIndex;
	}

	public List<Integer> getChainIndices() {
		return chainIndices;
	}

	public Optional<Integer> getDestinationActivityIndex() {
		return destinationActivityIndex;
	}

	static private List<Integer> getActivityIndices(ActivityIndices indices, List<PlanElement> planElements,
			boolean includeOriginDestination) {
		List<Integer> activityIndices = new LinkedList<>(indices.getChainIndices());

		if (includeOriginDestination && indices.getOriginActivityIndex().isPresent()) {
			activityIndices.add(0, indices.getOriginActivityIndex().get());
		}

		if (includeOriginDestination && indices.getDestinationActivityIndex().isPresent()) {
			activityIndices.add(indices.getDestinationActivityIndex().get());
		}

		return activityIndices;
	}

	static public List<Activity> getActivities(ActivityIndices indices, List<PlanElement> planElements,
			boolean includeOriginDestination) {
		return getActivityIndices(indices, planElements, includeOriginDestination).stream().map(planElements::get)
				.map(Activity.class::cast).collect(Collectors.toList());
	}

	static public List<Leg> getLegs(ActivityIndices indices, List<PlanElement> planElements,
			boolean includeOriginDestination) {
		return getActivityIndices(indices, planElements, includeOriginDestination).stream().skip(1).map(i -> i - 1)
				.map(planElements::get).map(Leg.class::cast).collect(Collectors.toList());
	}

	static public Optional<Activity> getOriginActivity(ActivityIndices indices, List<PlanElement> planElements) {
		return indices.getOriginActivityIndex().map(planElements::get).map(Activity.class::cast);
	}

	static public Optional<Activity> getDestinationActivity(ActivityIndices indices, List<PlanElement> planElements) {
		return indices.getDestinationActivityIndex().map(planElements::get).map(Activity.class::cast);
	}
}
