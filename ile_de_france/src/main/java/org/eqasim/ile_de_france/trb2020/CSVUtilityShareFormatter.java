package org.eqasim.ile_de_france.trb2020;

public class CSVUtilityShareFormatter {
	private String delimiter;

	public CSVUtilityShareFormatter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String formatHeader() {
		return String.join(delimiter,
				new String[] { "person_id",
						"accessEgressTime_min_car", "crowflyDistance_km_car", "cost_EUR_car", "travelTime_min_car",
						"accessEgressTime_min_pt", "cost_EUR_pt", "crowflyDistance_km_pt", "inVehicleTime_min_pt", "numberOfLineSwitches_pt", "waitingTime_min_pt",
						"travelTime_min_bike", "travelTime_min_walk" });
	}

	public String formatItem(UtilityShareItem item) {
		return String.join(delimiter, new String[] {
				item.personId.toString(),
				String.valueOf(item.carVariables.accessEgressTime_min),
				String.valueOf(item.carVariables.crowflyDistance_km),
				String.valueOf(item.carVariables.cost_EUR),
				String.valueOf(item.carVariables.travelTime_min),
				String.valueOf(item.ptVariables.accessEgressTime_min),
				String.valueOf(item.ptVariables.cost_EUR),
				String.valueOf(item.ptVariables.crowflyDistance_km),
				String.valueOf(item.ptVariables.inVehicleTime_min),
				String.valueOf(item.ptVariables.numberOfLineSwitches),
				String.valueOf(item.ptVariables.waitingTime_min),
				String.valueOf(item.bikeVariables.travelTime_min),
				String.valueOf(item.walkVariables.travelTime_min),
		});
	}
}
