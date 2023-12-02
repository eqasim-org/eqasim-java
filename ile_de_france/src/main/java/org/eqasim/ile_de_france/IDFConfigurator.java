package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        if (config.getModules().containsKey(MultiModeDrtConfigGroup.GROUP_NAME)) {
            MultiModeDrtConfigGroup multiModeDrtConfigGroup = (MultiModeDrtConfigGroup) config.getModules().get(MultiModeDrtConfigGroup.GROUP_NAME);
            Set<String> drtModes = multiModeDrtConfigGroup.modes().collect(Collectors.toSet());
            scenario.getPopulation().getPersons().values().stream()
                    .flatMap(p -> p.getSelectedPlan().getPlanElements().stream())
                    .filter(planElement -> planElement instanceof Leg)
                    .map(planElement -> (Leg) planElement)
                    .filter(leg->drtModes.contains(leg.getMode()))
                    .forEach(leg -> leg.setRoute(null));
        }
    }
}
