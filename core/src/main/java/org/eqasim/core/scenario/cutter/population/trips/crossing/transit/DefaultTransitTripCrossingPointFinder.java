package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPointFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.routes.TransitPassengerRoute;

import com.google.inject.Inject;

/**
 * TODO: Focused on "pt" legs, but others are possible
 */
public class DefaultTransitTripCrossingPointFinder implements TransitTripCrossingPointFinder {
	final private TransitRouteCrossingPointFinder transitFinder;
	final private TeleportationCrossingPointFinder walkFinder;

	@Inject
	public DefaultTransitTripCrossingPointFinder(TransitRouteCrossingPointFinder transitFinder,
			TeleportationCrossingPointFinder walkFinder) {
		this.transitFinder = transitFinder;
		this.walkFinder = walkFinder;
	}

	@Override
	public List<TransitTripCrossingPoint> findCrossingPoints(Coord startCoord, List<PlanElement> trip, Coord endCoord) {
		List<TransitTripCrossingPoint> result = new LinkedList<>();

		for (int i = 0; i < trip.size(); i++) {
			PlanElement element = trip.get(i);

			if (element instanceof Leg) {
				Leg leg = (Leg) element;

				switch (leg.getMode()) {
				case "walk":
					Coord legStartCoord = (i == 0) ? startCoord : ((Activity) trip.get(i - 1)).getCoord();
					Coord legEndCoord = (i == trip.size() - 1) ? endCoord : ((Activity) trip.get(i + 1)).getCoord();

					result.addAll(walkFinder
							.findCrossingPoints(legStartCoord, legEndCoord, leg.getTravelTime().seconds(),
									leg.getDepartureTime().seconds())
							.stream().map(p -> new TransitTripCrossingPoint(p)).collect(Collectors.toList()));
					break;
				case "pt":
					result.addAll(transitFinder
							.findCrossingPoints((TransitPassengerRoute) leg.getRoute(),
									leg.getDepartureTime().seconds())
							.stream().map(p -> new TransitTripCrossingPoint(p)).collect(Collectors.toList()));
					break;
				default:
					throw new IllegalStateException(String.format("Unknown mode: %s", leg.getMode()));
				}
			}
		}

		return result;
	}
}
