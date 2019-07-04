package org.eqasim.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.scenario.cutter.population.trips.crossing.network.NetworkCrossingPoint;
import org.eqasim.scenario.cutter.population.trips.crossing.network.NetworkCrossingPointFinder;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;

import com.google.inject.Inject;

public class NetworkTripProcessor implements TripProcessor {
	final private NetworkCrossingPointFinder crossingPointFinder;
	final private ScenarioExtent extent;

	@Inject
	public NetworkTripProcessor(NetworkCrossingPointFinder crossingPointFinder, ScenarioExtent extent) {
		this.crossingPointFinder = crossingPointFinder;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		Leg leg = (Leg) trip.get(0);
		NetworkRoute route = (NetworkRoute) leg.getRoute();

		return process(leg.getMode(), route, leg.getDepartureTime(),
				!extent.isInside(firstActivity.getCoord()) && !extent.isInside(secondActivity.getCoord()));
	}

	public List<PlanElement> process(String mode, NetworkRoute route, double departureTime, boolean allOutside) {
		List<NetworkCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(route, departureTime);

		if (crossingPoints.size() == 0) {
			return Arrays.asList(PopulationUtils.createLeg(allOutside ? "outside" : mode));
		} else {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? mode : "outside"));

			for (NetworkCrossingPoint point : crossingPoints) {
				Activity activity = PopulationUtils.createActivityFromLinkId("outside", point.link.getId());
				activity.setEndTime(point.leaveTime);
				result.add(activity);
				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : mode));
			}

			return result;
		}
	}
}
