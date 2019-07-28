package org.eqasim.switzerland.trb2020;

public class CSVUtilityShareFormatter {
	private String delimiter;

	public CSVUtilityShareFormatter(String delimiter) {
		this.delimiter = delimiter;
	}

	public String formatHeader() {
		return String.join(delimiter,
				new String[] { "person_id", "crowflyDistance", "selectedMode",
						"car_alpha", "car_travelTime", "car_accessEgressTime", "car_cost",
						"pt_alpha", "pt_accessEgressTime", "pt_inVehicleTime", "pt_numberOfLineSwitches", "pt_waitingTime", "pt_cost",
						"bike_alpha", "bike_travelTime", "bike_ageOver18",
						"walk_alpha", "walk_travelTime"
				});
	}

	public String formatItem(UtilityShareItem item) {
		return String.join(delimiter, new String[] {
				item.personId.toString(),
				String.valueOf(item.crowflyDistance),
				String.valueOf(item.selectedMode),
				String.valueOf(item.car.alpha),
				String.valueOf(item.car.travelTime),
				String.valueOf(item.car.accessEgressTime),
				String.valueOf(item.car.cost),
				String.valueOf(item.pt.alpha),
				String.valueOf(item.pt.accessEgressTime),
				String.valueOf(item.pt.inVehicleTime),
				String.valueOf(item.pt.numberOfLineSwitches),
				String.valueOf(item.pt.waitingTime),
				String.valueOf(item.pt.cost),
				String.valueOf(item.bike.alpha),
				String.valueOf(item.bike.travelTime),
				String.valueOf(item.bike.ageOver18),
				String.valueOf(item.walk.alpha),
				String.valueOf(item.walk.travelTime),
		});
	}
}
