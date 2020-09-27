package org.eqasim.switzerland.ovgk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class CalculateStopCategories {
	private class StopInfo {
		boolean hasCategoryA = false;
		boolean hasCategoryB = false;
		double depatures = 0;

		Set<TransitLine> railLines = new HashSet<>();
		Set<TransitStopFacility> facilities = new HashSet<>();
	}

	public void run(TransitSchedule schedule) {
		Set<String> categoryAModes = new HashSet<>(Arrays.asList("rail"));
		Set<String> categoryBModes = new HashSet<>(Arrays.asList("tram", "bus", "ferry", "metro"));

		// I) Prepare data structure
		Map<String, StopInfo> stopInfos = new HashMap<>();

		for (TransitStopFacility stop : schedule.getFacilities().values()) {
			String stopId = stop.getName().split(".link:")[0];

			if (!stopInfos.containsKey(stopId)) {
				stopInfos.put(stopId, new StopInfo());
			}

			stopInfos.get(stopId).facilities.add(stop);
		}

		// Find mode category (A, B, C) for each stop facility
		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				boolean hasCategoryA = categoryAModes.contains(route.getTransportMode());
				boolean hasCategoryB = categoryBModes.contains(route.getTransportMode());

				for (TransitRouteStop stop : route.getStops()) {
					String stopId = stop.getStopFacility().getName().split(".link:")[0];
					StopInfo info = stopInfos.get(stopId);

					info.hasCategoryA |= hasCategoryA;
					info.hasCategoryB |= hasCategoryB;

					if (route.getTransportMode().equals("rail")) {
						info.railLines.add(line);
					}

					double departureOffset = stop.getDepartureOffset().seconds();

					boolean isTerminus = route.getStops().get(0) == stop
							|| route.getStops().get(route.getStops().size() - 1) == stop;

					boolean isCycle = route.getStops().get(0).getStopFacility() == route.getStops()
							.get(route.getStops().size() - 1).getStopFacility();

					for (Departure departure : route.getDepartures().values()) {
						double stopDepartureTime = departure.getDepartureTime() + departureOffset;

						if (stopDepartureTime >= 6.0 * 3600.0 && stopDepartureTime <= 20.0 * 3600.0) {
							if (isTerminus && isCycle) {
								info.depatures += 0.25;
							} else {
								info.depatures += 0.5;
							}
						}
					}
				}
			}
		}

		// II) Aggergate information
		for (StopInfo info : stopInfos.values()) {
			// Check if it is a rail hub or just a station
			boolean isRailHub = info.railLines.size() > 1;

			// Find frequency
			double frequency_min = 840.0 / info.depatures;

			// Find stop category
			int stopCategory = OVGKConstants.WORST_STOP_CATEGORY;

			if (info.hasCategoryA) {
				if (frequency_min < 5.0) {
					stopCategory = 1;
				} else if (frequency_min < 10.0) {
					stopCategory = isRailHub ? 1 : 2;
				} else if (frequency_min < 20.0) {
					stopCategory = isRailHub ? 2 : 3;
				} else if (frequency_min < 40.0) {
					stopCategory = isRailHub ? 3 : 4;
				} else if (frequency_min <= 60.0) {
					stopCategory = isRailHub ? 4 : 5;
				}
			} else if (info.hasCategoryB) {
				if (frequency_min < 5.0) {
					stopCategory = 2;
				} else if (frequency_min < 10.0) {
					stopCategory = 3;
				} else if (frequency_min < 20.0) {
					stopCategory = 4;
				}
			}

			for (TransitStopFacility facility : info.facilities) {
				facility.getAttributes().putAttribute(OVGKConstants.STOP_CATEGORY_ATTRIBUTE, stopCategory);
			}
		}
	}
}
