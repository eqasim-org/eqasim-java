package org.eqasim.core.scenario.cutter.population.trips.crossing.transit;

import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPoint;

public class TransitTripCrossingPoint {
	final public boolean isInVehicle;
	final public boolean isOutgoing;

	final public TransitRouteCrossingPoint transitRoutePoint;
	final public TeleportationCrossingPoint teleportationPoint;

	public TransitTripCrossingPoint(TransitRouteCrossingPoint transitRoutePoint) {
		this.isInVehicle = true;
		this.transitRoutePoint = transitRoutePoint;
		this.teleportationPoint = null;
		this.isOutgoing = transitRoutePoint.isOutgoing;
	}

	public TransitTripCrossingPoint(TeleportationCrossingPoint teleportationPoint) {
		this.isInVehicle = false;
		this.transitRoutePoint = null;
		this.teleportationPoint = teleportationPoint;
		this.isOutgoing = teleportationPoint.isOutgoing;
	}
}
