package org.eqasim.switzerland.ch.utils.pricing.inputs.zonal;

import java.util.LinkedList;
import java.util.List;

public class ZonalStage {
    private final List<ZonalWaypoint> waypoints = new LinkedList<>();
	private final double departureTime;
	private final double arrivalTime;

	public ZonalStage(double departureTime, double arrivalTime) {
		this.departureTime = departureTime;
		this.arrivalTime = arrivalTime;
	}

	public void addWaypoint(ZonalWaypoint waypoint) {
		waypoints.add(waypoint);
	}

	public double getDepartureTime() {
		return departureTime;
	}

	public double getArrivalTime() {
		return arrivalTime;
	}

	public List<ZonalWaypoint> getWaypoints() {
		return waypoints;
	}
}
