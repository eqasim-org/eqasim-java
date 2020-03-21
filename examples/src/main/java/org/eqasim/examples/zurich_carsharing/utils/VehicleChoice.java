package org.eqasim.examples.zurich_carsharing.utils;

import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.vehicles.CSVehicle;

public class VehicleChoice implements VehicleChoiceAgent {

		
	public static final Logger log = Logger.getLogger(VehicleChoice.class);

	@Override
	public CSVehicle chooseVehicle(List<CSVehicle> vehicleOptions, Link startLink, Leg leg, double currentTime,
			Person person) {

		if (vehicleOptions.size() > 0)
			return vehicleOptions.get(0);
		

		return null;
	}

	@Override
	public CSVehicle chooseVehicleActivityTimeIncluded(List<CSVehicle> vehicleOptions, Link startLink, Leg leg,
			double currentTime, Person person, double durationOfNextActivity, boolean keepthecar) {
		return null;
	}
	
	
	
	
}
