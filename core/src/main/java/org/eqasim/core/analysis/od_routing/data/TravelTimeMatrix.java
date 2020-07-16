package org.eqasim.core.analysis.od_routing.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class TravelTimeMatrix {
	private final double[][] values;
	private final Map<Location, Integer> indices = new HashMap<>();

	public TravelTimeMatrix(Collection<Location> locations) {
		this.values = new double[locations.size()][locations.size()];

		int index = 0;

		for (Location location : locations) {
			indices.put(location, index);
			index++;
		}

		for (int i = 0; i < locations.size(); i++) {
			for (int j = 0; j < locations.size(); j++) {
				values[i][j] = Double.NaN;
			}
		}
	}

	public void setTravelTime(Location origin, Location destination, double value) {
		int originIndex = indices.get(origin);
		int destinationIndex = indices.get(destination);
		this.values[originIndex][destinationIndex] = value;
	}

	public double getTravelTime(Location origin, Location destination) {
		int originIndex = indices.get(origin);
		int destinationIndex = indices.get(destination);
		return this.values[originIndex][destinationIndex];
	}
}
