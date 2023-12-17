package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPoint;

public class NetworkTripCrossingPoint {
	final public boolean isInVehicle;
	final public boolean isOutgoing;

	final public NetworkRouteCrossingPoint networkRoutePoint;
	final public TeleportationCrossingPoint teleportationPoint;
	
	final public String legMode;

	public NetworkTripCrossingPoint(NetworkRouteCrossingPoint networkRoutePoint, String legMode) {
		this.isInVehicle = true;
		this.networkRoutePoint = networkRoutePoint;
		this.teleportationPoint = null;
		this.isOutgoing = networkRoutePoint.isOutgoing;
		this.legMode = legMode;
	}

	public NetworkTripCrossingPoint(TeleportationCrossingPoint teleportationPoint, String legMode) {
		this.isInVehicle = false;
		this.networkRoutePoint = null;
		this.teleportationPoint = teleportationPoint;
		this.isOutgoing = teleportationPoint.isOutgoing;
		this.legMode = legMode;
	}
}
