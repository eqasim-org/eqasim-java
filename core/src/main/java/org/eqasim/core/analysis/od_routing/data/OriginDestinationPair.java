package org.eqasim.core.analysis.od_routing.data;

public class OriginDestinationPair {
	private Location origin;
	private Location destination;

	public OriginDestinationPair(Location origin, Location destination) {
		this.origin = origin;
		this.destination = destination;
	}

	public Location getOrigin() {
		return origin;
	}

	public Location getDestination() {
		return destination;
	}
}
