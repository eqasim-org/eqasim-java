package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashMap;
import java.util.Map;

public class IDFConfigurator extends EqasimConfigurator {
    public void adjustScenario(Scenario scenario) {
        // if there is a vehicles file defined in config, manually assign them to their agents
        Config config = scenario.getConfig();
        if (config.qsim().getVehiclesSource() == QSimConfigGroup.VehiclesSource.fromVehiclesData) {
            for (Person person : scenario.getPopulation().getPersons().values()) {
                Id<Vehicle> vehicleId = Id.createVehicleId(person.getId());
                Map<String, Id<Vehicle>> modeVehicle = new HashMap<>();
                modeVehicle.put("car", vehicleId);
                VehicleUtils.insertVehicleIdsIntoAttributes(person, modeVehicle);
            }
        }
    }
}
