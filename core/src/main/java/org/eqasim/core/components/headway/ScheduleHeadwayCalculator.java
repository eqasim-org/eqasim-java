package org.eqasim.core.components.headway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class ScheduleHeadwayCalculator implements HeadwayCalculator {
	private final SwissRailRaptor raptor;
	private final TransitSchedule schedule;

	private IdMap<TransitRoute, List<Departure>> sortedDepartures = new IdMap<>(TransitRoute.class);

	public ScheduleHeadwayCalculator(SwissRailRaptor raptor, TransitSchedule schedule) {
		this.raptor = raptor;
		this.schedule = schedule;
	}

	private List<Departure> getSortedDepartures(TransitRoute transitRoute) {
		List<Departure> departures = sortedDepartures.get(transitRoute.getId());

		if (departures == null) {
			departures = new ArrayList<>(transitRoute.getDepartures().values().stream().sorted((a, b) -> {
				return Double.compare(a.getDepartureTime(), b.getDepartureTime());
			}).collect(Collectors.toList()));
			sortedDepartures.put(transitRoute.getId(), departures);
		}

		return departures;
	}

	double calculateNextArrival(List<Leg> legs) {
		List<TransitPassengerRoute> passengerRoutes = new LinkedList<>();
		for (Leg leg : legs) {
			if (leg.getRoute() instanceof TransitPassengerRoute) {
				passengerRoutes.add(((TransitPassengerRoute) leg.getRoute()));
			}
		}

		if (passengerRoutes.size() == 0) {
			return Double.POSITIVE_INFINITY;
		}

		Map<Tuple<Id<TransitStopFacility>, Id<TransitStopFacility>>, Double> transferTimes = new HashMap<>();

		for (int k = 1; k < legs.size() - 1; k++) {
			Leg precedingLeg = legs.get(k - 1);
			Leg followingLeg = legs.get(k + 1);

			if (precedingLeg.getRoute() instanceof TransitPassengerRoute) {
				if (followingLeg.getRoute() instanceof TransitPassengerRoute) {
					if (!(legs.get(k).getRoute() instanceof TransitPassengerRoute)) {
						TransitPassengerRoute precedingRoute = (TransitPassengerRoute) precedingLeg.getRoute();
						TransitPassengerRoute followingRoute = (TransitPassengerRoute) followingLeg.getRoute();

						var relation = Tuple.of(precedingRoute.getEgressStopId(), followingRoute.getAccessStopId());
						transferTimes.put(relation, legs.get(k).getTravelTime().seconds());
					}
				}
			}
		}

		double passengerTime = passengerRoutes.get(0).getBoardingTime().seconds() + 1.0;

		for (int passengerRouteIndex = 0; passengerRouteIndex < passengerRoutes.size(); passengerRouteIndex++) {
			TransitPassengerRoute passengerRoute = passengerRoutes.get(passengerRouteIndex);

			TransitLine transitLine = schedule.getTransitLines().get(passengerRoute.getLineId());

			TransitStopFacility accessFacility = schedule.getFacilities().get(passengerRoute.getAccessStopId());
			TransitStopFacility egressFacility = schedule.getFacilities().get(passengerRoute.getEgressStopId());

			double routeArrivalTime = Double.POSITIVE_INFINITY;

			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				TransitRouteStop accessStop = transitRoute.getStop(accessFacility);
				TransitRouteStop egressStop = transitRoute.getStop(egressFacility);

				if (accessStop != null && egressStop != null
						&& accessStop.getDepartureOffset().seconds() <= egressStop.getDepartureOffset().seconds()) {
					for (Departure departure : getSortedDepartures(transitRoute)) {
						double currentDepartureTime = departure.getDepartureTime()
								+ accessStop.getDepartureOffset().seconds();
						double currentArrivalTime = departure.getDepartureTime()
								+ egressStop.getArrivalOffset().seconds();

						if (currentDepartureTime >= passengerTime) {
							if (currentArrivalTime < routeArrivalTime) {
								routeArrivalTime = currentArrivalTime;
							}

							break;
						}
					}
				}
			}

			if (Double.isInfinite(routeArrivalTime)) {
				return Double.POSITIVE_INFINITY;
			} else {
				passengerTime = routeArrivalTime;

				if (passengerRouteIndex < passengerRoutes.size() - 1) {
					TransitPassengerRoute nextPassengerRoute = passengerRoutes.get(passengerRouteIndex + 1);
					var relation = Tuple.of(passengerRoute.getEgressStopId(), nextPassengerRoute.getAccessStopId());
					passengerTime += transferTimes.getOrDefault(relation, 0.0);
				}
			}
		}

		Leg lastLeg = legs.get(legs.size() - 1);
		if (!(lastLeg.getRoute() instanceof TransitPassengerRoute)) {
			passengerTime += lastLeg.getTravelTime().seconds();
		}

		return passengerTime;
	}

	public double calculateHeadway_min(Facility originFacility, Facility destinationFacility, double departureTime) {
		List<Leg> legs = raptor.calcRoute(originFacility, destinationFacility, departureTime, null);

		if (legs == null) {
			return Double.POSITIVE_INFINITY;
		}

		return calculateHeadway_min(originFacility, destinationFacility, departureTime, legs);
	}

	@Override
	public double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime,
			List<Leg> directRoute) {

		double initialArrivalTime = directRoute.get(directRoute.size() - 1).getDepartureTime().seconds()
				+ directRoute.get(directRoute.size() - 1).getTravelTime().seconds();

		double nextArrivalTime = calculateNextArrival(directRoute);

		return (nextArrivalTime - initialArrivalTime) / 60.0;
	}
}
