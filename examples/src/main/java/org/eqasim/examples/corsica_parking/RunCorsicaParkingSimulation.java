package org.eqasim.examples.corsica_parking;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.examples.corsica_parking.components.parking.ParkingListener;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilitiesFactory;
import org.matsim.facilities.ActivityFacilitiesFactoryImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

import java.util.LinkedList;
import java.util.List;

public class RunCorsicaParkingSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-choice-parameter", "cost-parameter", "withParking") //
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());
		config.addModule(new ParkingSearchConfigGroup());
		cmd.applyConfiguration(config);

		EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(config);
		eqasimConfigGroup.setEstimator(TransportMode.car, IDFModeChoiceModule.CAR_PARKING_ESTIMATOR_NAME);

		//get the parking search config group to set some parameters, like agent's search strategy or average parking slot length
		ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.DistanceMemory);
//		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.Benenson);

		config.controler().setWritePlansInterval(20);
		config.controler().setWriteEventsInterval(20);
		config.controler().setLastIteration(20);
//		config.controler().setOutputDirectory("/media/tchervec/LaCie/eqasim_output/distance_memory");
		config.controler().setOutputDirectory("/home/tchervec/git/eqasim-java/simulation_output/search_penalty");
//		config.plans().setInputFile("/media/tchervec/LaCie/eqasim_output/distance_memory/ITERS/it.56/56.plans.xml.gz");
//		config.controler().setFirstIteration(0);

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		// DynAgents require simulation to start from the very beginning.
		// Set 'QSim.simStarttimeInterpretation' to onlyUseStarttime
		scenario.getConfig().qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

		// Do not simulate PT
		scenario.getConfig().transit().setUseTransit(true);
		scenario.getConfig().transit().setUsingTransitInMobsim(false);

		// remove persons with only one activity
		List<Id<Person>> personIds = new LinkedList<>();
		for (Person person : scenario.getPopulation().getPersons().values()){
			if (person.getSelectedPlan().getPlanElements().size() == 1) {
				personIds.add(person.getId());
			}
		}
		for (Id<Person> personId : personIds){
			scenario.getPopulation().removePerson(personId);
		}

		// create 10 parking spaces per link
		ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				String parkingId = "parking_link_" + link.getId().toString();
				ActivityFacility activityFacility = facilitiesFactory.createActivityFacility(Id.create(parkingId, ActivityFacility.class),
						link.getCoord(), link.getId());
				ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
				activityOption.setCapacity(10);
				activityFacility.addActivityOption(activityOption);
				scenario.getActivityFacilities().addActivityFacility(activityFacility);
			}
		}

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);

		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		// Add parking search module
		controller.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
				addEventHandlerBinding().toInstance(visualiser);
				addControlerListenerBinding().toInstance(visualiser);
			}
		});
//		if (otfvis) {
//			controler.addOverridingModule(new OTFVisLiveModule());
//		}
		SetupParking.installParkingModules(controller);

		controller.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				Vehicle2DriverEventHandler vehicle2DriverEventHandler = new Vehicle2DriverEventHandler();
				ParkingListener parkingListener = new ParkingListener(vehicle2DriverEventHandler, new RoadNetwork(scenario.getNetwork()),
						0.0, 30*3600.0, 3600.0, 500.0);

				bind(ParkingListener.class).toInstance(parkingListener);
				addEventHandlerBinding().toInstance(vehicle2DriverEventHandler);
				addEventHandlerBinding().toInstance(parkingListener);
				addControlerListenerBinding().toInstance(parkingListener);
			}
		});

		controller.run();
	}
}