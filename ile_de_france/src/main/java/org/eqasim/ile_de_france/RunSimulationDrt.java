package org.eqasim.ile_de_france;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eqasim.core.scenario.validation.VehiclesValidator;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.core.simulation.modes.drt.mode_choice.DrtModeAvailabilityWrapper;
import org.eqasim.core.simulation.modes.drt.mode_choice.rejections.RejectionTrackerModule;
import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.ile_de_france.mode_choice.IDFModeAvailability;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.eqasim.ile_de_france.munich.MunichModeChoiceModule;
import org.eqasim.ile_de_france.policies.PolicyExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contribs.discrete_mode_choice.components.constraints.ShapeFileConstraint.Requirement;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.modules.ConstraintModule;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
import org.matsim.contribs.discrete_mode_choice.modules.config.ShapeFileConstraintConfigGroup;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;

public class RunSimulationDrt {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "use-vdf", "use-vdf-engine") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"));
		configurator.updateConfig(config);

		if (cmd.getOption("use-vdf").map(Boolean::parseBoolean).orElse(false)) {
			config.qsim().setFlowCapFactor(1e9);
			config.qsim().setStorageCapFactor(1e9);

			VDFConfigGroup vdfConfig = new VDFConfigGroup();
			config.addModule(vdfConfig);

			vdfConfig.setCapacityFactor(0.5);
			vdfConfig.setModes(Set.of("car", "car_passenger"));

			if (cmd.getOption("use-vdf-engine").map(Boolean::parseBoolean).orElse(false)) {
				VDFEngineConfigGroup engineConfig = new VDFEngineConfigGroup();
				engineConfig.setModes(Set.of("car", "car_passenger"));
				engineConfig.setGenerateNetworkEvents(false);
				config.addModule(engineConfig);

				config.qsim().setMainModes(Collections.emptySet());
			}
		}

		cmd.applyConfiguration(config);
		VehiclesValidator.validate(config);

		PolicyExtension policies = new PolicyExtension();
		policies.adaptConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		DrtConfigGroup drtConfig = DrtConfigGroup.getSingleModeDrtConfig(config);

		DiscreteModeChoiceConfigGroup dmcConfig = DiscreteModeChoiceConfigGroup.getOrCreate(config);
		ShapeFileConstraintConfigGroup areaConfig = dmcConfig.getShapeFileConstraintConfigGroup();
		areaConfig.setConstrainedModes(Collections.singleton("drt"));
		areaConfig.setRequirement(Requirement.BOTH);
		areaConfig.setPath(drtConfig.drtServiceAreaShapeFile);

		Set<String> constraints = new HashSet<>(dmcConfig.getTripConstraints());
		constraints.add(ConstraintModule.SHAPE_FILE);
		dmcConfig.setTripConstraints(constraints);

		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(config);
		Set<String> modes = new HashSet<>(terminationConfig.getModes());
		modes.add("drt");
		terminationConfig.setModes(new LinkedList<>(modes));

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
		controller.addOverridingModule(policies);
		controller.addOverridingModule(new RejectionTrackerModule());

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(ModeAvailability.class)
						.toInstance(new DrtModeAvailabilityWrapper(config, new IDFModeAvailability()));
			}
		});

		controller.run();
	}
}