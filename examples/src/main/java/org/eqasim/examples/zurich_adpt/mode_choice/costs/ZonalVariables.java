package org.eqasim.examples.zurich_adpt.mode_choice.costs;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.core.utils.io.IOUtils;

public class ZonalVariables {

	
	private Map<String, Map<String, Double>> zoneZoneCosts;
	private Map<String, Map<String, Double>> zoneZoneFrequency;

	public void readFile(String filePath) throws IOException {
		
		BufferedReader reader = IOUtils.getBufferedReader(filePath);
		
		//read header
		reader.readLine();
		String s = reader.readLine();
		zoneZoneCosts = new HashMap<>();
		zoneZoneFrequency = new HashMap<>();
		while (s != null) {
			
			String[] values = s.split(",");
			
			String startId = values[0];
			String endId = values[1];
			double cost = Double.parseDouble(values[2]);
			if (zoneZoneCosts.containsKey(startId)) {
				zoneZoneCosts.get(startId).put(endId, cost);
			}
			else {
				Map<String, Double> newEntry = new HashMap<>();
				zoneZoneCosts.put(startId, newEntry);
				zoneZoneCosts.get(startId).put(endId, cost);

			}
			
			s = reader.readLine();
		}
	}

	public Map<String, Map<String, Double>> getZoneZoneCosts() {
		return zoneZoneCosts;
	}

	public Map<String, Map<String, Double>> getZoneZoneFrequency() {
		return zoneZoneFrequency;
	}
}
