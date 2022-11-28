package org.eqasim.ile_de_france.parking;

import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPredictorUtils;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

public class ParkingInformation {
	private final IdMap<Person, Boolean> hasUrbanHome = new IdMap<>(Person.class);

	private final double urbanParkingCost_EUR_h;
	private final ParkingPressureData parkingPressure;

	public ParkingInformation(ParkingPressureData parkingPressure, double urbanParkingCost_EUR_h) {
		this.parkingPressure = parkingPressure;
		this.urbanParkingCost_EUR_h = urbanParkingCost_EUR_h;
	}

	private boolean hasUrbanHome(Person person) {
		Boolean cache = hasUrbanHome.get(person.getId());

		if (cache != null) {
			return cache;
		} else {
			for (Activity activity : TripStructureUtils.getActivities(person.getSelectedPlan(),
					StageActivityHandling.ExcludeStageActivities)) {
				if (activity.getType().equals("home")) {
					if (IDFPredictorUtils.isUrbanArea(activity)) {
						hasUrbanHome.put(person.getId(), true);
						return true;
					}
				}
			}

			hasUrbanHome.put(person.getId(), false);
			return false;
		}
	}

	public double getParkingCost_EUR_h(Activity activity, Person person) {
		if (IDFPredictorUtils.isUrbanArea(activity) && !hasUrbanHome(person)) {
			return urbanParkingCost_EUR_h;
		}

		return 0.0;
	}

	public double getParkingPressure(Id<Link> linkId) {
		return parkingPressure.getParkingPressure(linkId);
	}
}
