package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoute;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.google.inject.Inject;

/*
 * TODO: This class should make use of StopSequenceCrossingPointFinder
 */
public class DefaultTransitRouteCrossingPointFinder implements TransitRouteCrossingPointFinder {
	final private ScenarioExtent extent;
	final private TransitSchedule schedule;

	@Inject
	public DefaultTransitRouteCrossingPointFinder(ScenarioExtent extent, TransitSchedule schedule) {
		this.extent = extent;
		this.schedule = schedule;
	}

	@Override
	public List<TransitRouteCrossingPoint> findCrossingPoints(EnrichedTransitRoute route, double departureTime) {
		List<TransitRouteCrossingPoint> crossingPoints = new LinkedList<>();

		TransitLine transitLine = schedule.getTransitLines().get(route.getLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(route.getRouteId());

		Departure departure = transitRoute.getDepartures().get(route.getDepartureId());
		double routeDepartureTime = departure.getDepartureTime();

		List<TransitRouteStop> stops = transitRoute.getStops().subList(route.getAccessStopIndex(),
				route.getEgressStopIndex() + 1);

		for (int i = 0; i < stops.size() - 1; i++) {
			TransitRouteStop firstStop = stops.get(i);
			TransitRouteStop secondStop = stops.get(i + 1);

			boolean firstIsInside = extent.isInside(firstStop.getStopFacility().getCoord());
			boolean secondIsInside = extent.isInside(secondStop.getStopFacility().getCoord());

			if (firstIsInside != secondIsInside) { // We found a crossing
				TransitRouteStop insideStop = firstIsInside ? firstStop : secondStop;
				TransitRouteStop outsideStop = firstIsInside ? secondStop : firstStop;

				double insideDepartureTime = routeDepartureTime;
				double outsideDepartureTime = routeDepartureTime;

				// This happens if we cross the border only to reach the very last stop of the
				// line.
				if (insideStop.getDepartureOffset().isDefined()) {
					insideDepartureTime += insideStop.getDepartureOffset().seconds();
				} else if (insideStop.getArrivalOffset().isDefined()) {
					insideDepartureTime += insideStop.getArrivalOffset().seconds();
				} else {
					throw new IllegalStateException();
				}

				if (outsideStop.getDepartureOffset().isDefined()) {
					outsideDepartureTime += outsideStop.getDepartureOffset().seconds();
				} else if (outsideStop.getArrivalOffset().isDefined()) {
					outsideDepartureTime += outsideStop.getArrivalOffset().seconds();
				} else {
					throw new IllegalStateException();
				}

				crossingPoints.add(new TransitRouteCrossingPoint(transitLine, transitRoute, outsideStop, insideStop,
						outsideDepartureTime, insideDepartureTime, firstIsInside));
			}
		}

		return crossingPoints;
	}
}
