package org.eqasim.ile_de_france.scenario;

import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class RunInsertVehicles {
	static public void main(String[] args) throws UncheckedIOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "population-path", "output-path") //
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));

		Vehicles vehicles = scenario.getVehicles();
		VehiclesFactory factory = vehicles.getFactory();

		vehicles.addVehicleType(VehicleUtils.getDefaultVehicleType());

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Map<String, Id<Vehicle>> personVehicles = new HashMap<>();

			for (String mode : config.routing().getNetworkModes()) {
				Vehicle vehicle = factory.createVehicle(Id.createVehicleId(person.getId().toString() + ":" + mode),
						VehicleUtils.getDefaultVehicleType());
				vehicles.addVehicle(vehicle);

				personVehicles.put(mode, vehicle.getId());
			}

			VehicleUtils.insertVehicleIdsIntoAttributes(person, personVehicles);
		}

		new MatsimVehicleWriter(vehicles).writeFile(cmd.getOptionStrict("output-path"));
	}
}
