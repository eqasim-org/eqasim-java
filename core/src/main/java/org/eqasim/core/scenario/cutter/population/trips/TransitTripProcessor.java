package org.eqasim.core.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPointFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.PopulationUtils;

public class TransitTripProcessor implements TripProcessor {
	final private TransitTripCrossingPointFinder transitPointFinder;
	final private double departureTimeBuffer;
	final private ScenarioExtent extent;

	public TransitTripProcessor(TransitTripCrossingPointFinder transitPointFinder, ScenarioExtent extent,
			double departureTimeBuffer) {
		this.transitPointFinder = transitPointFinder;
		this.departureTimeBuffer = departureTimeBuffer;
		this.extent = extent;
	}

	@Override
	public List<PlanElement> process(Activity firstActivity, List<PlanElement> trip, Activity secondActivity) {
		return process(firstActivity.getCoord(), trip, secondActivity.getCoord(),
				!extent.isInside(firstActivity.getCoord()) && !extent.isInside(secondActivity.getCoord()));
	}

	public List<PlanElement> process(Coord firstCoord, List<PlanElement> trip, Coord secondCoord, boolean allOutside) {
		List<TransitTripCrossingPoint> crossingPoints = transitPointFinder.findCrossingPoints(firstCoord, trip,
				secondCoord);

		if (crossingPoints.size() == 0) {
			return Arrays.asList(PopulationUtils.createLeg(allOutside ? "outside" : "pt"));
		} else {
			List<PlanElement> result = new LinkedList<>();

			result.add(PopulationUtils.createLeg(crossingPoints.get(0).isOutgoing ? "pt" : "outside"));

			for (TransitTripCrossingPoint point : crossingPoints) {
				if (point.isInVehicle) {
					Activity activity = PopulationUtils.createActivityFromCoord("outside",
							point.transitRoutePoint.insideStop.getStopFacility().getCoord());
					activity.setEndTime(point.transitRoutePoint.insideDepartureTime - departureTimeBuffer);
					result.add(activity);
				} else {
					Activity activity = PopulationUtils.createActivityFromCoord("outside",
							point.teleportationPoint.coord);
					activity.setEndTime(point.teleportationPoint.time);
					result.add(activity);
				}

				result.add(PopulationUtils.createLeg(point.isOutgoing ? "outside" : "pt"));
			}

			return result;
		}
	}
}
