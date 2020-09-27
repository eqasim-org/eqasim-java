package org.eqasim.core.simulation.calibration;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eqasim.core.analysis.TripItem;
import org.eqasim.core.analysis.TripListener;

public class CalibrationDataProvider {
	private final TripListener tripListener;
	private final CalibrationData reference;

	public CalibrationDataProvider(TripListener tripListener, CalibrationData reference) {
		this.tripListener = tripListener;
		this.reference = reference;
	}

	public CalibrationData getData() {
		Collection<String> modes = reference.totalModeShare.keySet();
		TripCountTracker tracker = new TripCountTracker(reference.totalModeShare.keySet(),
				reference.distanceBoundaries);

		for (TripItem trip : tripListener.getTripItems()) {
			if (trip.precedingPurpose.equals("outside")) {
				continue;
			}

			if (trip.followingPurpose.equals("outside")) {
				continue;
			}

			if (trip.mode.equals("outside")) {
				continue;
			}
			
			if (trip.euclideanDistance == 0.0) {
				continue;
			}

			tracker.addObservation(trip.mode, trip.euclideanDistance);
		}

		CalibrationData data = new CalibrationData();
		data.distanceBoundaries = reference.distanceBoundaries;

		double totalNumberOfTrips = tracker.getCounts().values().stream().flatMap(Collection::stream)
				.mapToDouble(d -> d).sum();

		for (String mode : modes) {
			double modeNumberOfTrips = tracker.getCounts().get(mode).stream().mapToDouble(d -> d).sum();
			data.totalModeShare.put(mode, modeNumberOfTrips / totalNumberOfTrips);
		}

		for (int i = 0; i < data.distanceBoundaries.size() + 1; i++) {
			Map<String, Double> binDistribution = new HashMap<>();

			final int _i = i;
			double distanceNumberOfTrips = tracker.getCounts().values().stream().mapToDouble(b -> b.get(_i)).sum();

			for (String mode : modes) {
				binDistribution.put(mode, tracker.getCounts().get(mode).get(i) / distanceNumberOfTrips);
			}
			
			data.modeShareByDistance.add(binDistribution);
		}

		return data;
	}
}
