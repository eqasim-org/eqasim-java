package org.eqasim.examples.covid19;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

public class VehicleFinder {
	private final TransitSchedule transitSchedule;
	private final Map<Tuple<Id<TransitLine>, Id<TransitRoute>>, List<Departure>> orderedDepartures = new HashMap<>();

	public VehicleFinder(TransitSchedule transitSchedule) {
		this.transitSchedule = transitSchedule;

		for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Departure> departures = new LinkedList<>();

				for (Departure departure : transitRoute.getDepartures().values()) {
					departures.add(departure);
				}

				Collections.sort(departures, new Comparator<Departure>() {
					@Override
					public int compare(Departure a, Departure b) {
						return Double.compare(a.getDepartureTime(), b.getDepartureTime());
					}
				});

				orderedDepartures.put(createId(transitLine, transitRoute), departures);
			}
		}
	}

	private Tuple<Id<TransitLine>, Id<TransitRoute>> createId(TransitLine transitLine, TransitRoute transitRoute) {
		return new Tuple<>(transitLine.getId(), transitRoute.getId());
	}

	public Id<Vehicle> findVehicleId(PublicTransitEvent event) {
		TransitLine transitLine = transitSchedule.getTransitLines().get(event.getTransitLineId());
		TransitRoute transitRoute = transitLine.getRoutes().get(event.getTransitRouteId());

		List<Departure> routeDepartures = orderedDepartures.get(createId(transitLine, transitRoute));

		List<Double> offsets = transitRoute.getStops().stream()
				.filter(stop -> stop.getStopFacility().getId().equals(event.getAccessStopId()))
				.map(TransitRouteStop::getDepartureOffset).collect(Collectors.toList());

		for (Departure departure : routeDepartures) {
			for (double offset : offsets) {
				if (departure.getDepartureTime() + offset == event.getVehicleDepartureTime()) {
					return departure.getVehicleId();
				}
			}
		}

		throw new IllegalStateException("Could not find departure!");
	}
}
