package org.eqasim;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ControllerConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup.LinkDynamics;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;
import org.matsim.vehicles.PersonVehicles;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class TestMotorcycles {

	@Before
	public void setUp() throws IOException {
		URL fixtureUrl = getClass().getResource("/melun");
		FileUtils.copyDirectory(new File(fixtureUrl.getPath()), new File("melun_test_motorcycle/input"));
	}

	@After
	public void tearDown() throws IOException {
		 FileUtils.deleteDirectory(new File("melun_test_motorcycle"));
	}

	private void runAddMotorcycles() {
		Vehicles vehicles = VehicleUtils.createVehiclesContainer();
		new MatsimVehicleReader(vehicles).readFile("melun_test_motorcycle/input/vehicles.xml.gz");

		VehicleType motorcycleType = VehicleUtils.createVehicleType(Id.create("motorcycleType", VehicleType.class));
		motorcycleType.setNetworkMode(TransportMode.motorcycle);
		vehicles.addVehicleType(motorcycleType);

		for (Vehicle vehicle: vehicles.getVehicles().values()) {
			Id<Vehicle> id = vehicle.getId();
			if (id.toString().endsWith("car")) {
				Id<Vehicle> new_id = Id.create(id.toString().replace("car", "motorcycle"), Vehicle.class);
				Vehicle new_vehicle = VehicleUtils.createVehicle(new_id, motorcycleType);
				vehicles.addVehicle(new_vehicle);
			}
		}

		MatsimVehicleWriter writer = new MatsimVehicleWriter(vehicles);
		writer.writeFile("melun_test_motorcycle/input/vehicles.xml.gz");
	}

	private void runModifyConfig() {
		Config config = ConfigUtils.loadConfig("melun_test_motorcycle/input/config.xml");
		config.controller().setOutputDirectory("melun_test_motorcycle/output");

		Collection<String> mainModes = new HashSet<>(config.qsim().getMainModes());
		mainModes.add(TransportMode.motorcycle);
		config.qsim().setMainModes(mainModes);
		
		config.qsim().setLinkDynamics(LinkDynamics.SeepageQ);

		Collection<String> seepModes = new HashSet<>(config.qsim().getSeepModes());
		seepModes.add(TransportMode.motorcycle);
		config.qsim().setSeepModes(seepModes);

		ConfigUtils.writeConfig(config, "melun_test_motorcycle/input/config.xml");
	}

	private void runModifyNetwork() {
		Config config = ConfigUtils.loadConfig("melun_test_motorcycle/input/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()) {
			Set<String> modes = new HashSet<>(link.getAllowedModes());
			if (modes.contains("car")) {
				modes.add("motorcycle");
				link.setAllowedModes(modes);
			}
		}
		NetworkUtils.writeNetwork(network, "melun_test_motorcycle/input/network.xml.gz");
	}
	
	private void runModifyPlans() {
		Config config = ConfigUtils.loadConfig("melun_test_motorcycle/input/config.xml");
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Population population = scenario.getPopulation();

		int replaceOnceEvery = 10;
		int counter = 0;

		for (Person person : population.getPersons().values()) {
			Id<Person> personId = person.getId();
			PersonVehicles person_vehicles = (PersonVehicles) person.getAttributes().getAttribute("vehicles");
			person_vehicles.addModeVehicleIfAbsent(TransportMode.motorcycle, Id.create(personId.toString() + ":motorcycle", Vehicle.class));

			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Leg) {
						Leg leg = (Leg) element;
						if (leg.getMode().equals("car")) {
							counter += 1;
							if (counter >= replaceOnceEvery) {
								counter = 0;
								leg.setMode("motorcycle");
							}
						}
					}
				}
			}
		}

		PopulationWriter writer = new PopulationWriter(population);
		writer.write("melun_test_motorcycle/input/population.xml.gz");
	}

	private void runMelunSimulation() throws ConfigurationException {
		EqasimConfigurator eqasimConfigurator = new TestConfigurator();
		Config config = ConfigUtils.loadConfig("melun_test_motorcycle/input/config.xml");
		eqasimConfigurator.updateConfig(config);
		((ControllerConfigGroup) config.getModules().get(ControllerConfigGroup.GROUP_NAME))
				.setOutputDirectory("melun_test_motorcycle/output");

		Scenario scenario = ScenarioUtils.createScenario(config);
		eqasimConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		eqasimConfigurator.adjustScenario(scenario);

		Controler controller = new Controler(scenario);
		eqasimConfigurator.configureController(controller);
		controller.run();

		Map<String, Long> counts = countLegs("melun_test_motorcycle/output/output_events.xml.gz");
		Assert.assertEquals(427, (long) counts.get("motorcycle"));
	}

	@Test
	public void runTestMotorcycles() throws CommandLine.ConfigurationException, IOException {
		runAddMotorcycles();
		runModifyConfig();
		runModifyNetwork();
		runModifyPlans();
		runMelunSimulation();
	}

	static Map<String, Long> countLegs(String eventsPath) {
		EventsManager manager = EventsUtils.createEventsManager();

		Map<String, Long> counts = new HashMap<>();
		manager.addHandler((PersonDepartureEventHandler) event -> {
			counts.compute(event.getLegMode(), (k, v) -> v == null ? 1 : v + 1);
		});

		new MatsimEventsReader(manager).readFile(eventsPath);

		System.out.println("Counts:");
		for (Map.Entry<String, Long> entry : counts.entrySet()) {
			System.out.println("  " + entry.getKey() + " " + entry.getValue());
		}

		return counts;
	}
}
