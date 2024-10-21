package org.eqasim.ile_de_france;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.VehicleTourConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup.AccessEgressType;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class RunSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "use-vdf") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		
		if (cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false)) {
			EqasimConfigGroup eqasimConfig = EqasimConfigGroup.get(config);
			
			VDFConfigGroup vdfConfig = new VDFConfigGroup();
			config.addModule(vdfConfig);
			
			vdfConfig.setCapacityFactor(eqasimConfig.getSampleSize());
			vdfConfig.setModes(Set.of("car", "car_passenger"));
			
			VDFEngineConfigGroup engineConfig = new VDFEngineConfigGroup();
			engineConfig.setModes(Set.of("car", "car_passenger"));
			engineConfig.setGenerateNetworkEvents(false);			
			config.addModule(engineConfig);		}
		
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
		controller.run();
	}
}