package org.eqasim.core.simulation.calibration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TripCountTracker {
	private final List<Double> boundaries;
	private final Map<String, List<Double>> counts = new HashMap<>();

	public TripCountTracker(Collection<String> modes, List<Double> boundaries) {
		this.boundaries = boundaries;

		for (String mode : modes) {
			counts.put(mode, new ArrayList<>(Collections.nCopies(boundaries.size() + 1, 0.0)));
		}
	}

	static public int getBin(double distance, List<Double> boundaries) {
		int bin = 0;

		for (double boundary : boundaries) {
			if (distance > boundary) {
				bin++;
			}
		}

		return bin;
	}

	public void addObservation(String mode, double distance) {
		int bin = getBin(distance, boundaries);

		if (counts.containsKey(mode)) {
			counts.get(mode).set(bin, counts.get(mode).get(bin) + 1);
		}
	}

	public Map<String, List<Double>> getCounts() {
		return counts;
	}
}
