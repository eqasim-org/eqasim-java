package org.eqasim.core.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPointFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

import com.google.inject.Inject;

public class TeleportationTripProcessor implements TripProcessor {
	final private TeleportationCrossingPointFinder crossingPointFinder;
	final private ScenarioExtent extent;

	@Inject
	public TeleportationTripProcessor(TeleportationCrossingPointFinder crossingPointFinder, ScenarioExtent extent) {
		this.crossingPointFinder = crossingPointFinder;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		Leg leg = (Leg) trip.get(0);

		return process(firstActivity.getCoord(), secondActivity.getCoord(), leg.getTravelTime().seconds(),
				leg.getDepartureTime().seconds(), leg.getMode(),
				!extent.isInside(firstActivity.getCoord()) && !extent.isInside(secondActivity.getCoord()));
	}

	public List<PlanElement> process(Coord firstCoord, Coord secondCoord, double travelTime, double departureTime,
			String mode, boolean allOutside) {
		List<TeleportationCrossingPoint> crossingPoints = crossingPointFinder.findCrossingPoints(firstCoord,
				secondCoord, travelTime, departureTime);

		if (crossingPoints.size() == 0) {
			return Arrays.asList(PopulationUtils.createLeg(allOutside ? "outside" : mode));
		} else {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? mode : "outside"));

			for (TeleportationCrossingPoint point : crossingPoints) {
				Activity activity = PopulationUtils.createActivityFromCoord("outside", point.coord);
				activity.setEndTime(point.time);
				result.add(activity);
				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : mode));
			}

			return result;
		}
	}
}
