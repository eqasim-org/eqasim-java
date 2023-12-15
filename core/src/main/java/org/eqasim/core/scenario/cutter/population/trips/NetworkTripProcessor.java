package org.eqasim.core.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkTripCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkTripCrossingPointFinder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;

public class NetworkTripProcessor implements TripProcessor {
	final private NetworkTripCrossingPointFinder crossingPointFinder;
	final private ScenarioExtent extent;

	@Inject
	public NetworkTripProcessor(NetworkTripCrossingPointFinder crossingPointFinder, ScenarioExtent extent) {
		this.crossingPointFinder = crossingPointFinder;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Id<Person> personId, int firstLegIndex, Activity firstActivity,
			List<PlanElement> trip, Activity secondActivity, String routingMode) {
		List<NetworkTripCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(personId, firstLegIndex,
				firstActivity.getCoord(), trip, secondActivity.getCoord());

		if (crossingPoints.size() > 0) { // there are crossing points
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils
					.createLeg(crossingPoints.get(0).isOutgoing ? crossingPoints.get(0).legMode : "outside"));

			for (NetworkTripCrossingPoint point : crossingPoints) {
				if (point.isInVehicle) {
					Activity activity = PopulationUtils.createActivityFromLinkId("outside",
							point.networkRoutePoint.link.getId());
					activity.setEndTime(point.networkRoutePoint.leaveTime);
					result.add(activity);
				} else {
					Activity activity = PopulationUtils.createActivityFromCoord("outside",
							point.teleportationPoint.coord);
					activity.setEndTime(point.teleportationPoint.time);
					result.add(activity);
				}

				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : point.legMode));
			}

			return result;
		} else { // there are no crossing points
			if (crossingPointFinder.isInside(trip)) { // whole trip is inside
				return Arrays.asList(PopulationUtils.createLeg(routingMode));
			} else {
				// The route is outside. This does not mean that both (or any) activity is
				// actually outside. These are mainly special cases in which the route is
				// outside, but some activity is inside, across the border, because the link is
				// parallel to the border. We put an outside activity right next to the inside
				// activity.

				List<PlanElement> result = new LinkedList<>();

				result.add(PopulationUtils.createLeg("outside"));

				if (extent.isInside(firstActivity.getCoord())) {
					Activity activity = PopulationUtils.createActivityFromLinkId("outside", firstActivity.getLinkId());
					activity.setEndTime(firstActivity.getEndTime().seconds());
					result.add(activity);

					result.add(PopulationUtils.createLeg("outside"));
				}

				if (extent.isInside(secondActivity.getCoord())) {
					Activity activity = PopulationUtils.createActivityFromLinkId("outside", secondActivity.getLinkId());
					activity.setEndTime(secondActivity.getStartTime().seconds());
					result.add(activity);

					result.add(PopulationUtils.createLeg("outside"));
				}

				return result;
			}
		}
	}
}
