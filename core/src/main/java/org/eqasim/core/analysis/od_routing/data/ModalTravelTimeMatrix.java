package org.eqasim.core.analysis.od_routing.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModalTravelTimeMatrix {
	private final Map<String, TravelTimeMatrix> matrices = new HashMap<>();

	public ModalTravelTimeMatrix(Collection<Location> locations, Collection<String> modes) {
		for (String mode : modes) {
			matrices.put(mode, new TravelTimeMatrix(locations));
		}
	}

	public void setValue(String mode, Location origin, Location destination, double value) {
		matrices.get(mode).setTravelTime(origin, destination, value);
	}

	public double getValue(String mode, Location origin, Location destination) {
		return matrices.get(mode).getTravelTime(origin, destination);
	}
}
