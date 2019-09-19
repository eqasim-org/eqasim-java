package org.eqasim.core.simulation.calibration;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class CalibrationData {
	public Map<String, Double> totalModeShare = new HashMap<>();

	public List<Double> distanceBoundaries = new LinkedList<>();
	public List<Map<String, Double>> modeShareByDistance = new LinkedList<>();
}
