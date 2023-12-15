package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPointFinder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;

import com.google.inject.Inject;

public class DefaultNetworkTripCrossingPointFinder implements NetworkTripCrossingPointFinder {
	final private NetworkRouteCrossingPointFinder networkFinder;
	final private TeleportationCrossingPointFinder walkFinder;

	@Inject
	public DefaultNetworkTripCrossingPointFinder(NetworkRouteCrossingPointFinder networkFinder,
			TeleportationCrossingPointFinder walkFinder) {
		this.networkFinder = networkFinder;
		this.walkFinder = walkFinder;
	}

	@Override
	public List<NetworkTripCrossingPoint> findCrossingPoints(Id<Person> personId, int firstLegIndex, Coord startCoord,
			List<PlanElement> trip, Coord endCoord) {
		List<NetworkTripCrossingPoint> result = new LinkedList<>();
		int legIndex = firstLegIndex;

		for (int i = 0; i < trip.size(); i++) {
			PlanElement element = trip.get(i);

			if (element instanceof Leg) {
				Leg leg = (Leg) element;
				Route route = leg.getRoute();

				if (route instanceof NetworkRoute) {
					result.addAll(networkFinder
							.findCrossingPoints(personId, legIndex, leg.getMode(), (NetworkRoute) route,
									leg.getDepartureTime().seconds())
							.stream().map(p -> new NetworkTripCrossingPoint(p, leg.getMode()))
							.collect(Collectors.toList()));
				} else {
					Coord legStartCoord = (i == 0) ? startCoord : ((Activity) trip.get(i - 1)).getCoord();
					Coord legEndCoord = (i == trip.size() - 1) ? endCoord : ((Activity) trip.get(i + 1)).getCoord();

					result.addAll(walkFinder
							.findCrossingPoints(legStartCoord, legEndCoord, leg.getTravelTime().seconds(),
									leg.getDepartureTime().seconds())
							.stream().map(p -> new NetworkTripCrossingPoint(p, leg.getMode()))
							.collect(Collectors.toList()));
				}

				legIndex++;
			}
		}

		return result;
	}

	@Override
	public boolean isInside(List<PlanElement> trip) {
		for (Leg leg : TripStructureUtils.getLegs(trip)) {
			if (leg.getRoute() instanceof NetworkRoute) {
				if (!networkFinder.isInside((NetworkRoute) leg.getRoute())) {
					return false;
				}
			}
		}

		return true;
	}
}
