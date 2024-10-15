package org.eqasim.ile_de_france;

import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.munich.MunichModeChoiceModule;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		configurator.addOptionalConfigGroups(config);
		cmd.applyConfiguration(config);
		VehiclesValidator.validate(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		/*-{
			// TODO: Make this static! > OK looks like everything is covered in pipeline
			Vehicles vehicles = scenario.getVehicles();
			VehiclesFactory factory = vehicles.getFactory();

			VehicleType vehicleType = vehicles.getVehicleTypes()
					.get(Id.create("defaultVehicleType", VehicleType.class));

			// ok should be done in pipeline
			for (Person person : scenario.getPopulation().getPersons().values()) {
				Map<String, Id<Vehicle>> personVehicles = new HashMap<>();

				for (String mode : Arrays.asList("passenger")) {
					Vehicle vehicle = factory.createVehicle(Id.createVehicleId(person.getId().toString() + ":" + mode),
							vehicleType);
					vehicles.addVehicle(vehicle);

					personVehicles.put(mode, vehicle.getId());
				}

				VehicleUtils.insertVehicleIdsIntoPersonAttributes(person, personVehicles);
			}

			for (Person person : scenario.getPopulation().getPersons().values()) {
				person.getAttributes().putAttribute("bicycleAvailability",
						person.getAttributes().getAttribute("bikeAvailability")); // ok done in pipeline

				// ok mode changes in pipeline
				for (Plan plan : person.getPlans()) {
					for (Leg leg : TripStructureUtils.getLegs(plan)) {
						if (leg.getMode().equals("bike")) {
							leg.setMode("bicycle");
							TripStructureUtils.setRoutingMode(leg, "bicycle");
						}
					}
				}
			}

			// OK done with pt2matsim
			for (Link link : scenario.getNetwork().getLinks().values()) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());

				if (allowedModes.contains("car")) {
					allowedModes.add("passenger");
				}

				link.setAllowedModes(allowedModes);
			}
		}*/

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));
		controller.addOverridingModule(new MunichModeChoiceModule());
		controller.run();
	}
}