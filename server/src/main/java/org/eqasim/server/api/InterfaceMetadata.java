package org.eqasim.server.api;

import org.geotools.referencing.CRS;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Metadata is computed once from the networks and schedule used by the services. */
public record InterfaceMetadata(
		String crs,
		@JsonProperty("projection_definition") String projectionDefinition,
		@JsonProperty("network_extent") Extent networkExtent,
		@JsonProperty("schedule_day_count") long scheduleDayCount) {
	public record Extent(
			@JsonProperty("min_x") double minX, @JsonProperty("min_y") double minY,
			@JsonProperty("max_x") double maxX, @JsonProperty("max_y") double maxY) {
	}

	public static InterfaceMetadata create(Network originalNetwork, Network roadNetwork, TransitSchedule schedule) {
		String crs = ProjectionUtils.getCRS(originalNetwork);
		if (crs == null || crs.isBlank()) {
			throw new IllegalArgumentException("Cannot initialize interface: scenario network has no CRS attribute.");
		}
		String definition;
		try {
			// GeoTools emits WKT1, supported by proj4js, including units and datum parameters.
			// Force longitude/easting first, matching MATSim's service x/y convention.
			definition = CRS.decode(crs, true).toWKT();
		} catch (Exception e) {
			throw new IllegalArgumentException("Cannot initialize interface: cannot produce a browser projection for " + crs, e);
		}

		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for (var node : roadNetwork.getNodes().values()) {
			double x = node.getCoord().getX(), y = node.getCoord().getY();
			if (!Double.isFinite(x) || !Double.isFinite(y)) {
				throw new IllegalArgumentException("Cannot initialize interface: car network contains non-finite node coordinates.");
			}
			minX = Math.min(minX, x);
			minY = Math.min(minY, y);
			maxX = Math.max(maxX, x);
			maxY = Math.max(maxY, y);
		}
		if (!(minX < maxX && minY < maxY)) {
			throw new IllegalArgumentException("Cannot initialize interface: cleaned car network is empty or has a degenerate extent.");
		}
		return new InterfaceMetadata(crs, definition, new Extent(minX, minY, maxX, maxY), scheduleDayCount(schedule));
	}

	public static long scheduleDayCount(TransitSchedule schedule) {
		double latest = 0;
		if (schedule != null) {
			for (var line : schedule.getTransitLines().values()) {
				for (var route : line.getRoutes().values()) {
					for (var departure : route.getDepartures().values()) {
						for (var stop : route.getStops()) {
							for (var offset : new double[] { stop.getArrivalOffset().orElse(Double.NaN),
									stop.getDepartureOffset().orElse(Double.NaN) }) {
								double event = departure.getDepartureTime() + offset;
								if (Double.isFinite(event)) {
									latest = Math.max(latest, event);
								}
							}
						}
					}
				}
			}
		}
		return Math.max(1, (long) Math.floor(latest / 86400) + 1);
	}
}
