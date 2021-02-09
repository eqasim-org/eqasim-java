package org.eqasim.examples.zurich_adpt.mode_choice.costs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class ZonalVariables {

	private Map<String, Map<String, Double>> zoneZoneCosts;
	private Map<String, Map<String, Double>> zoneZoneHeadway;

	public void readFile(String filePath) throws IOException {

		BufferedReader reader = IOUtils.getBufferedReader(filePath);

		// read header
		reader.readLine();
		String s = reader.readLine();
		zoneZoneCosts = new HashMap<>();
		zoneZoneHeadway = new HashMap<>();
		while (s != null) {

			String[] values = s.split(",");

			String startId = values[0];
			String endId = values[1];
			double cost = Double.parseDouble(values[2]);
			double headway = Double.parseDouble(values[3]);
			if (zoneZoneCosts.containsKey(startId)) {
				zoneZoneCosts.get(startId).put(endId, cost);
				zoneZoneHeadway.get(startId).put(endId, headway);
			} else {
				Map<String, Double> newEntry = new HashMap<>();
				zoneZoneCosts.put(startId, newEntry);
				zoneZoneCosts.get(startId).put(endId, cost);
				Map<String, Double> newEntryHeadway = new HashMap<>();
				zoneZoneHeadway.put(startId, newEntryHeadway);
				zoneZoneHeadway.get(startId).put(endId, headway);

			}

			s = reader.readLine();
		}
	}

	public Map<String, Map<String, Double>> getZoneZoneCosts() {
		return zoneZoneCosts;
	}

	public Map<String, Map<String, Double>> getZoneZoneFrequency() {
		return zoneZoneHeadway;
	}
}
