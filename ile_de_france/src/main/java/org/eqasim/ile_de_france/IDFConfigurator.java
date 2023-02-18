package org.eqasim.ile_de_france;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

public class IDFConfigurator extends EqasimConfigurator {
	public void adjustScenario(Scenario scenario) {
		// if there is a vehicles file defined in config, manually assign them to their
		// agents
		Config config = scenario.getConfig();
		if (config.qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Id<Vehicle> vehicleId = Id.createVehicleId(person.getId());
				Map<String, Id<Vehicle>> modeVehicle = new HashMap<>();
				modeVehicle.put("car", vehicleId);
				VehicleUtils.insertVehicleIdsIntoAttributes(person, modeVehicle);
			}
		}

		// Assign the routing mode
		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				Set<String> modes = new HashSet<>();
				TripStructureUtils.getLegs(plan).forEach(leg -> modes.add(leg.getMode()));

				String routingMode = "walk";

				for (String mode : modes) {
					if (mode.startsWith("pt")) {
						routingMode = "pt";
					}
				}

				if (modes.contains("car")) {
					routingMode = "car";
				}

				if (modes.contains("car_passenger")) {
					routingMode = "car_passenger";
				}

				if (modes.contains("bike")) {
					routingMode = "bike";
				}

				final String finalRoutingMode = routingMode;
				TripStructureUtils.getLegs(plan)
						.forEach(leg -> TripStructureUtils.setRoutingMode(leg, finalRoutingMode));
			}
		}
	}
}
