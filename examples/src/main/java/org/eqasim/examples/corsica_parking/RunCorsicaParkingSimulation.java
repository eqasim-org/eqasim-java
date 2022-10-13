package org.eqasim.examples.corsica_parking;

import com.google.inject.Key;
import com.google.inject.name.Names;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.eqasim.core.simulation.analysis.EqasimAnalysisModule;
import org.eqasim.core.simulation.mode_choice.EqasimModeChoiceModule;
import org.eqasim.examples.corsica_parking.components.parking.ParkingListener;
import org.eqasim.examples.corsica_parking.mode_choice.CorsicaParkingModule;
import org.eqasim.ile_de_france.IDFConfigurator;
import org.eqasim.ile_de_france.mode_choice.IDFModeChoiceModule;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.dvrp.router.DvrpGlobalRoutingNetworkProvider;
import org.matsim.contrib.dvrp.router.DvrpModeRoutingModule;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModes;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.evaluation.ParkingSlotVisualiser;
import org.matsim.contrib.parking.parkingsearch.manager.FacilityBasedParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.MultipleParkingTypeParkingManager;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacilityType;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationLogic;
import org.matsim.contrib.parking.parkingsearch.manager.vehicleteleportationlogic.VehicleTeleportationToNearbyParking;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contrib.parking.parkingsearch.routing.WithinDayParkingRouter;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchPopulationModule;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchQSimModule;
import org.matsim.contrib.parking.parkingsearch.sim.SetupParking;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.mobsim.qsim.PopulationModule;
import org.matsim.core.mobsim.qsim.components.QSimComponentsConfig;
import org.matsim.core.mobsim.qsim.components.StandardQSimComponentConfigurator;
import org.matsim.core.router.AStarEuclideanFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.*;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class RunCorsicaParkingSimulation {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path") //
				.allowPrefixes("mode-parameter", "cost-parameter") //
				.allowOptions("output-path")
				.build();

		IDFConfigurator configurator = new IDFConfigurator();
		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), configurator.getConfigGroups());

		config.controler().setWritePlansInterval(5);
		config.controler().setWriteEventsInterval(5);
		config.controler().setLastIteration(5);
		if (cmd.getOption("output-path").isPresent()) {
			config.controler().setOutputDirectory(cmd.getOptionStrict("output-path"));
		}

		{ // Configure parking
			ParkingSearchConfigGroup parkingSearchConfigGroup = new ParkingSearchConfigGroup();

			// set parameters
			parkingSearchConfigGroup.setParkingSearchStrategy(ParkingSearchStrategy.Random);

			// add to config
			config.addModule(parkingSearchConfigGroup);

			// additional requirements
			config.qsim().setSimStarttimeInterpretation(QSimConfigGroup.StarttimeInterpretation.onlyUseStarttime);

			// Do not simulate PT
			config.transit().setUseTransit(true);
			config.transit().setUsingTransitInMobsim(false);

		}

		cmd.applyConfiguration(config);

		{ // Add parking to mode choice
			EqasimConfigGroup eqasimConfigGroup = EqasimConfigGroup.get(config);
			eqasimConfigGroup.setEstimator("car", CorsicaParkingModule.CAR_ESTIMATOR_NAME);
		}

		Scenario scenario = ScenarioUtils.createScenario(config);
		configurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		configurator.adjustScenario(scenario);

		{ // clean up scenario
			// add parking at home for 90% of households and parking at work for 50% of people
			Random random = new Random(1);
			ActivityFacilitiesFactory facilitiesFactory = new ActivityFacilitiesFactoryImpl();

			for (Household household : scenario.getHouseholds().getHouseholds().values()) {
				boolean hasParkingAtHome = random.nextInt(1) < 0.9;

				for (Id<Person> personId : household.getMemberIds()) {
					boolean hasParkingAtWork = random.nextInt(1) < 0.5;

					Leg previousLeg = null;

					for (PlanElement element : scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements()) {
						if (element instanceof Activity) {
							if (((Activity) element).getType().equals("home")) {

								element.getAttributes().putAttribute("parkingAvailable", hasParkingAtHome);

								if (hasParkingAtHome) {

									String parkingId = "dedicated_parking_facility_" + ((Activity) element).getFacilityId().toString();
									Id<ActivityFacility> activityFacilityId = Id.create(parkingId, ActivityFacility.class);
									element.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);

									ActivityFacilities activityFacilities = scenario.getActivityFacilities();
									if (!activityFacilities.getFacilities().containsKey(activityFacilityId)) {
										ActivityFacility activityFacility = facilitiesFactory.createActivityFacility(activityFacilityId,
												((Activity) element).getCoord(), ((Activity) element).getLinkId());
										ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
										activityOption.setCapacity(Integer.MAX_VALUE);
										activityFacility.addActivityOption(activityOption);
										activityFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.DedicatedParking.toString());
										Set<Id<Vehicle>> allowedVehicles = new HashSet<>();
										allowedVehicles.add(Id.createVehicleId(personId));
										activityFacility.getAttributes().putAttribute("allowedVehicles", allowedVehicles);
										scenario.getActivityFacilities().addActivityFacility(activityFacility);
									} else {
										((Set<Id<Vehicle>>) activityFacilities.getFacilities().get(activityFacilityId).getAttributes().getAttribute("allowedVehicles")).add(Id.createVehicleId(personId));
									}

									if (previousLeg != null) {
										if (previousLeg.getMode().equals("car")) {
											previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.DriveToParkingFacility.toString());
											previousLeg.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);
										}
									}
								}

							}
							else if (((Activity) element).getType().equals("work")) {
								element.getAttributes().putAttribute("parkingAvailable", hasParkingAtWork);

								if (hasParkingAtWork) {

									String parkingId = "dedicated_parking_facility_" + ((Activity) element).getFacilityId().toString();
									Id<ActivityFacility> activityFacilityId = Id.create(parkingId, ActivityFacility.class);
									element.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);

									ActivityFacilities activityFacilities = scenario.getActivityFacilities();
									if (!activityFacilities.getFacilities().containsKey(activityFacilityId)) {
										ActivityFacility activityFacility = facilitiesFactory.createActivityFacility(activityFacilityId,
												((Activity) element).getCoord(), ((Activity) element).getLinkId());
										ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
										activityOption.setCapacity(Integer.MAX_VALUE);
										activityFacility.addActivityOption(activityOption);
										activityFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.DedicatedParking.toString());
										Set<Id<Vehicle>> allowedVehicles = new HashSet<>();
										allowedVehicles.add(Id.createVehicleId(personId));
										activityFacility.getAttributes().putAttribute("allowedVehicles", allowedVehicles);
										scenario.getActivityFacilities().addActivityFacility(activityFacility);
									} else {
										((Set<Id<Vehicle>>) activityFacilities.getFacilities().get(activityFacilityId).getAttributes().getAttribute("allowedVehicles")).add(Id.createVehicleId(personId));
									}

									if (previousLeg != null) {
										if (previousLeg.getMode().equals("car")) {
											previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.DriveToParkingFacility.toString());
											previousLeg.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);
										}
									}
								}
							}
							else {
								if (previousLeg != null) {
									if (previousLeg.getMode().equals("car")) {
										previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.Random.toString());
									}
								}
							}
						}
						else if (element instanceof Leg) {
							previousLeg = (Leg) element;
						}
					}
				}
			}

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

			// create 10 blue-zone parking spaces per link
			for (Link link : scenario.getNetwork().getLinks().values()) {
				if (link.getAllowedModes().contains("car")) {
					String parkingId = "blue_parking_link_" + link.getId().toString();
					ActivityFacility activityFacility = facilitiesFactory.createActivityFacility(Id.create(parkingId, ActivityFacility.class),
							link.getCoord(), link.getId());
					ActivityOption activityOption = facilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
					activityOption.setCapacity(10);
					activityFacility.addActivityOption(activityOption);
					activityFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.BlueZone.toString());
					scenario.getActivityFacilities().addActivityFacility(activityFacility);
				}
			}

		}

		Controler controller = new Controler(scenario);
		configurator.configureController(controller);
		controller.addOverridingModule(new EqasimAnalysisModule());
		controller.addOverridingModule(new EqasimModeChoiceModule());
		controller.addOverridingModule(new IDFModeChoiceModule(cmd));

		{// Configure controller for parking
			controller.addOverridingModule(new CorsicaParkingModule(cmd));

//			// Add parking search module
//			controller.addOverridingModule(new AbstractModule() {
//				@Override
//				public void install() {
//					ParkingSlotVisualiser visualiser = new ParkingSlotVisualiser(scenario);
//					addEventHandlerBinding().toInstance(visualiser);
//					addControlerListenerBinding().toInstance(visualiser);
//				}
//			});

			// No need to route car routes in Routing module in advance, as they are
			// calculated on the fly
			if (!controller.getConfig().getModules().containsKey(DvrpConfigGroup.GROUP_NAME)) {
				controller.getConfig().addModule(new DvrpConfigGroup());
			}

			controller.addOverridingModule(new DvrpTravelTimeModule());
			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					bind(TravelDisutilityFactory.class).annotatedWith(DvrpModes.mode(TransportMode.car))
							.toInstance(TimeAsTravelDisutility::new);
					bind(Network.class).annotatedWith(DvrpModes.mode(TransportMode.car))
							.to(Key.get(Network.class, Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING)));
					install(new DvrpModeRoutingModule(TransportMode.car, new AStarEuclideanFactory()));
					bind(Network.class).annotatedWith(Names.named(DvrpGlobalRoutingNetworkProvider.DVRP_ROUTING))
							.to(Network.class)
							.asEagerSingleton();
					bind(ParkingSearchManager.class).to(MultipleParkingTypeParkingManager.class).asEagerSingleton();
					this.install(new ParkingSearchQSimModule());
					addControlerListenerBinding().to(org.matsim.contrib.parking.parkingsearch.evaluation.ParkingListener.class);
					bind(ParkingRouter.class).to(WithinDayParkingRouter.class);
					bind(VehicleTeleportationLogic.class).to(VehicleTeleportationToNearbyParking.class);
				}
			});

			controller.addOverridingModule(new AbstractModule() {
				@Override
				public void install() {
					QSimComponentsConfig components = new QSimComponentsConfig();

					new StandardQSimComponentConfigurator(controller.getConfig()).configure(components);
					components.removeNamedComponent(PopulationModule.COMPONENT_NAME);
					components.addNamedComponent(ParkingSearchPopulationModule.COMPONENT_NAME);

					bind(QSimComponentsConfig.class).toInstance(components);
				}
			});

			// Add parking listeners
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
		}

		controller.run();
	}
}