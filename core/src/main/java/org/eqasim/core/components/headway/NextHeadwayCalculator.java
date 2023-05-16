package org.eqasim.core.components.headway;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class NextHeadwayCalculator implements HeadwayCalculator {
	private final SwissRailRaptor raptor;
	private final double interval;
	private final double delay = 60.0;

	public NextHeadwayCalculator(SwissRailRaptor raptor, double interval) {
		this.raptor = raptor;
		this.interval = interval;
	}

	static private class TripElement {
		double departureTime;
		double boardingTime;
		double arrivalTime;
	}

	private TripElement performRouting(Facility originFacility, Facility destinationFacility, double departureTime) {
		List<Leg> legs = raptor.calcRoute(originFacility, destinationFacility, departureTime, null);

		if (legs == null) {
			return null;
		}

		LinkedList<Leg> transitLegs = new LinkedList<>();

		legs.forEach(leg -> {
			if (leg.getRoute() instanceof TransitPassengerRoute) {
				transitLegs.add(leg);
			}
		});

		if (transitLegs.size() == 0) {
			return null;
		}

		TripElement element = new TripElement();
		element.departureTime = departureTime;
		element.boardingTime = ((TransitPassengerRoute) transitLegs.getFirst().getRoute()).getBoardingTime().seconds();
		element.arrivalTime = legs.get(legs.size() - 1).getDepartureTime().seconds()
				+ legs.get(legs.size() - 1).getTravelTime().seconds();
		return element;
	}

	public double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime) {
		TripElement currentElement = performRouting(originFacility, destinationFacilty, departureTime);
		TripElement nextElement = currentElement;

		if (currentElement != null) {
			while (nextElement.departureTime - currentElement.departureTime < interval) {
				double nextOffset = currentElement.boardingTime - currentElement.departureTime + delay;
				nextElement = performRouting(originFacility, destinationFacilty, departureTime + nextOffset);

				if (nextElement == null) {
					break;
				}

				if (nextElement.arrivalTime <= currentElement.arrivalTime) {
					currentElement = nextElement; // Found a dominating solution
				} else {
					return (nextElement.departureTime - currentElement.departureTime) / 60.0;
				}
			}
		}

		return Double.POSITIVE_INFINITY;
	}

	@Override
	public double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime,
			List<Leg> directRoute) {
		return calculateHeadway_min(originFacility, destinationFacilty, departureTime);
	}
}
