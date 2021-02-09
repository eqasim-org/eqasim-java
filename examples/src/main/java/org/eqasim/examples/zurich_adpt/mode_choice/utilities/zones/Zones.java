package org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones;

import java.util.Map;

public class Zones {

	private Map<String, Zone> zones;

	public Zones(Map<String, Zone> zones) {
		this.zones = zones;
	}

	public Map<String, Zone> getZones() {
		return zones;
	}
}
