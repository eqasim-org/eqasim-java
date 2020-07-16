package org.eqasim.core.analysis.od_routing.data;

public class ModalOriginDestinationPair {
	private String mode;
	private Location origin;
	private Location destination;

	public ModalOriginDestinationPair(String mode, Location origin, Location destination) {
		this.mode = mode;
		this.origin = origin;
		this.destination = destination;
	}

	public ModalOriginDestinationPair(String mode, OriginDestinationPair pair) {
		this.mode = mode;
		this.origin = pair.getOrigin();
		this.destination = pair.getDestination();
	}

	public Location getOrigin() {
		return origin;
	}

	public Location getDestination() {
		return destination;
	}

	public String getMode() {
		return mode;
	}
}
