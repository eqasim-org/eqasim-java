package org.eqasim.core.scenario.routing;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.core.misc.InjectorBuilder;
import org.eqasim.core.simulation.EqasimConfigurator;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.VehiclesSource;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretationModule;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

import com.google.inject.Injector;

public class RunPopulationRouting {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "output-path") //
				.allowOptions("threads", "batch-size", "modes") //
				.build();

		EqasimConfigurator configurator = new EqasimConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
        config.getModules().remove(EqasimTerminationConfigGroup.GROUP_NAME);
		configurator.addOptionalConfigGroups(config);
		cmd.applyConfiguration(config);
		config.strategy().clearStrategySettings();

		int batchSize = cmd.getOption("batch-size").map(Integer::parseInt).orElse(100);
		int numberOfThreads = cmd.getOption("threads").map(Integer::parseInt)
				.orElse(Runtime.getRuntime().availableProcessors());

		Scenario scenario = ScenarioUtils.loadScenario(config);
		insertVehicles(config, scenario);

		if (scenario.getActivityFacilities() != null) {
			for (ActivityFacility facility : scenario.getActivityFacilities().getFacilities().values()) {
				if (facility.getLinkId() == null) {
					throw new IllegalStateException("Expecting facilities to have link IDs!");
				}
			}
		}

		Set<String> modes = new HashSet<>();

		if (cmd.hasOption("modes")) {
			for (String mode : cmd.getOptionStrict("modes").split(",")) {
				modes.add(mode.trim());
			}
		}

		Injector injector = new InjectorBuilder(scenario) //
				.addOverridingModules(configurator.getModules()) //
				.addOverridingModule(new PopulationRouterModule(numberOfThreads, batchSize, true, modes)) //
				.addOverridingModule(new TimeInterpretationModule()).build();

		PopulationRouter populationRouter = injector.getInstance(PopulationRouter.class);
		populationRouter.run(scenario.getPopulation());

		clearVehicles(config, scenario);
		new PopulationWriter(scenario.getPopulation()).write(cmd.getOptionStrict("output-path"));
	}

	static public void insertVehicles(Config config, Scenario scenario) {
		if (config.qsim().getVehiclesSource().equals(VehiclesSource.defaultVehicle)) {
			Vehicles vehicles = scenario.getVehicles();
			VehiclesFactory factory = vehicles.getFactory();

			vehicles.addVehicleType(VehicleUtils.getDefaultVehicleType());

			for (Person person : scenario.getPopulation().getPersons().values()) {
				Map<String, Id<Vehicle>> personVehicles = new HashMap<>();

				for (String mode : config.plansCalcRoute().getNetworkModes()) {
					Vehicle vehicle = factory.createVehicle(Id.createVehicleId(person.getId().toString() + ":" + mode),
							VehicleUtils.getDefaultVehicleType());
					vehicles.addVehicle(vehicle);

					personVehicles.put(mode, vehicle.getId());
				}

				VehicleUtils.insertVehicleIdsIntoAttributes(person, personVehicles);
			}
		}
	}

	static public void clearVehicles(Config config, Scenario scenario) {
		if (config.qsim().getVehiclesSource().equals(VehiclesSource.defaultVehicle)) {
			for (Person person : scenario.getPopulation().getPersons().values()) {
				person.getAttributes().removeAttribute("vehicles");

				for (Plan plan : person.getPlans()) {
					for (Leg leg : TripStructureUtils.getLegs(plan)) {
						if (leg.getRoute() instanceof NetworkRoute) {
							NetworkRoute route = (NetworkRoute) leg.getRoute();
							route.setVehicleId(null);
						}
					}
				}
			}
		}
	}
}
