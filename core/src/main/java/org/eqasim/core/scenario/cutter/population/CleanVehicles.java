package org.eqasim.core.scenario.cutter.population;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class CleanVehicles {
	private final IdSet<Vehicle> retainedIds = new IdSet<>(Vehicle.class);

	public CleanVehicles(Population population) {
		for (Person person : population.getPersons().values()) {
			retainedIds.addAll(VehicleUtils.getVehicleIds(person).values());
		}
	}

	public void run(Vehicles vehicles) {
		IdSet<Vehicle> removeIds = new IdSet<>(Vehicle.class);
		removeIds.addAll(vehicles.getVehicles().keySet());
		removeIds.removeAll(retainedIds);

		removeIds.forEach(vehicles::removeVehicle);
	}
}
