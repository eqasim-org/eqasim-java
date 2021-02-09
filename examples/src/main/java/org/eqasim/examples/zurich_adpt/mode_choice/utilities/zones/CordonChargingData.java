package org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones;

import java.util.Map;

public class CordonChargingData {

	private Map<Integer, Double> mapCordCharges;
	private Map<String, Zone> mapCordon;
	
	public CordonChargingData(Map<Integer, Double> mapCordCharges, Map<String, Zone> mapCordon) {

		this.mapCordCharges = mapCordCharges;
		this.mapCordon = mapCordon;
	}

	public Map<Integer, Double> getMapCordCharges() {
		return mapCordCharges;
	}

	public Map<String, Zone> getMapCordon() {
		return mapCordon;
	}
	
	
	

}
