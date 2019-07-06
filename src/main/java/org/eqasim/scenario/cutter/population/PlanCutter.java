package org.eqasim.scenario.cutter.population;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.misc.Constants;
import org.eqasim.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.scenario.cutter.population.trips.TripProcessor;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;

public class PlanCutter {
	private final ScenarioExtent extent;
	private final TripProcessor tripProcessor;
	private final StageActivityTypes stageActivityTypes;
	private final MergeOutsideActivities mergeOutsideActivities;

	@Inject
	public PlanCutter(TripProcessor tripProcessor, ScenarioExtent extent, StageActivityTypes stageActivityTypes,
			MergeOutsideActivities mergeOutsideActivities) {
		this.extent = extent;
		this.tripProcessor = tripProcessor;
		this.stageActivityTypes = stageActivityTypes;
		this.mergeOutsideActivities = mergeOutsideActivities;
	}

	private void addActivity(List<PlanElement> plan, Activity activity) {
		if (extent.isInside(activity.getCoord())) {
			plan.add(activity);
		} else {
			Activity virtualActivity = PopulationUtils.createActivityFromCoord(Constants.OUTSIDE_ACTIVITY_TYPE,
					activity.getCoord());
			virtualActivity.setEndTime(activity.getEndTime());
			virtualActivity.getAttributes().putAttribute(Constants.TYPE_BEFORE_CUTTING_ATTRIBUTE, activity.getType());

			plan.add(virtualActivity);
		}
	}

	public List<PlanElement> processPlan(List<PlanElement> elements) {
		List<PlanElement> result = new LinkedList<>();

		if (elements.size() > 0) {
			addActivity(result, (Activity) elements.get(0));

			for (TripStructureUtils.Trip trip : TripStructureUtils.getTrips(elements, stageActivityTypes)) {
				result.addAll(tripProcessor.process(trip.getOriginActivity(), trip.getTripElements(),
						trip.getDestinationActivity()));
				addActivity(result, trip.getDestinationActivity());
			}
		}

		mergeOutsideActivities.run(result);

		return result;
	}
}
