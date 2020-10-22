package org.eqasim.core.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPointFinder;
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
		List<NetworkCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(leg.getMode(), route,
				leg.getDepartureTime().seconds());

		if (crossingPoints.size() > 0) {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? leg.getMode() : "outside"));

			for (NetworkCrossingPoint point : crossingPoints) {
				Activity activity = PopulationUtils.createActivityFromLinkId("outside", point.link.getId());
				activity.setEndTime(point.leaveTime);
				result.add(activity);
				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : leg.getMode()));
			}

			return result;
		} else if (crossingPointFinder.isInside(route)) {
			return Arrays.asList(PopulationUtils.createLeg(leg.getMode()));
		} else {
			// The route is outside. This does not means that both (or any) activity is
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

	public List<PlanElement> process(String mode, NetworkRoute route, double departureTime, boolean allOutside) {
		List<NetworkCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(mode, route, departureTime);

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
