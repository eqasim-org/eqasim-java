package org.eqasim;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.simulation.vdf.VDFConfigGroup;
import org.eqasim.core.simulation.vdf.VDFModule;
import org.eqasim.core.simulation.vdf.VDFQSimModule;
import org.eqasim.core.simulation.vdf.engine.VDFEngineConfigGroup;
import org.eqasim.core.simulation.vdf.engine.VDFEngineModule;
import org.eqasim.core.simulation.vdf.travel_time.VDFLinkSpeedCalculator;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.drt.optimizer.insertion.extensive.ExtensiveInsertionSearchParams;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.DrtConfigs;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtModule;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.FleetSpecification;
import org.matsim.contrib.dvrp.fleet.FleetSpecificationImpl;
import org.matsim.contrib.dvrp.fleet.ImmutableDvrpVehicleSpecification;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEvent;
import org.matsim.contrib.dvrp.passenger.PassengerDroppedOffEventHandler;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeModule;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpModule;
import org.matsim.contrib.dvrp.run.DvrpQSimComponents;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup.StarttimeInterpretation;
import org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.config.groups.ScoringConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.Controller;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

/**
 * This test checks various requirements on the VDF simulation. There are the
 * following configurations:
 * 
 * - Only VDF travel time tracking and running the classic QSim with inifite
 * capacities, but travel times mandated by the VDF calculation.
 * 
 * - A) Using the VDF engine which skips the queue-based simulation and provides
 * simpler simulation mechanisms.
 * 
 * - B1) Dynamic mode: vehicles are tracked time step by time step like in the
 * QSim. This mode handles well situations in which within-day replanning is
 * used for the agents or for dynamic modes (DVRP is simulated by default using
 * this mode).
 * - C1) Static mode: agents' routes are examined and the time step by time step
 * simulation is avoided. Most of the calculation is performed upon departure.
 * 
 * By default, both modes generate events in every iteration. However, you can
 * decide to only generate network events (enter traffic, leave traffic, link
 * enter, link leave) in a configurable frequency of iterations. Not generating
 * these events should have another performance impact.
 * 
 * - B2) Dynamic mode without events: dynamic trajectories are still followed
 * but the respective events are not generated
 * - C2) Agents are basically teleported in one go to the destination according
 * to the expected travel time.
 * 
 * This test guarantees the following observations:
 * - In freeflow, the travel times of VDF and QSim produce equal travel times.
 * - In congested conditions, all five VDF configurations produce equal travel
 * times.
 * 
 * Note that travel times between the congested QSim and congested VDF are
 * generally not equal, because the simulation logic is completely different.
 * 
 * Note that public transport is implemented as a special case, but the same
 * guarantees from above apply. Likewise, traffic events for public transport
 * vehicles can be generated or not according to the chosen option.
 */
public class TestVolumeDelayFunction {
	private final static int LINKS = 20;
	private final static double FREESPEED = 10.0;
	private final static double CAPACITY = 50.0;
	private final static int ITERATIONS = 2;
	private final static double VDF_CAPACITY_FACTOR = 0.3;
	private final static String OUTPUT_PATH = "__test_output";

	private Controller prepareScenario() {
		Config config = ConfigUtils.createConfig();

		config.controller().setOutputDirectory(OUTPUT_PATH);
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(ITERATIONS);

		ActivityParams startParams = new ActivityParams("start");
		startParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(startParams);

		ActivityParams endParams = new ActivityParams("end");
		endParams.setScoringThisActivityAtAll(false);
		config.scoring().addActivityParams(endParams);

		StrategySettings rerouteStrategy = new StrategySettings();
		rerouteStrategy.setWeight(1.0);
		rerouteStrategy.setStrategyName("ReRoute");
		config.replanning().addStrategySettings(rerouteStrategy);

		config.addModule(new EqasimConfigGroup());

		Scenario scenario = ScenarioUtils.createScenario(config);

		Network network = scenario.getNetwork();
		NetworkFactory networkFactory = network.getFactory();

		List<Node> nodes = new ArrayList<>(LINKS + 1);
		for (int i = 0; i < LINKS + 1; i++) {
			Node node = networkFactory.createNode(Id.createNodeId(i), new Coord(1000.0 * i, 0.0));
			network.addNode(node);
			nodes.add(node);
		}

		for (int i = 0; i < LINKS; i++) {
			Link forward = networkFactory.createLink(Id.createLinkId("f" + i), nodes.get(i), nodes.get(i + 1));
			forward.setCapacity(CAPACITY);
			forward.setFreespeed(FREESPEED);
			network.addLink(forward);

			Link backward = networkFactory.createLink(Id.createLinkId("b" + i), nodes.get(i + 1), nodes.get(i));
			backward.setCapacity(CAPACITY);
			backward.setFreespeed(FREESPEED);
			network.addLink(backward);
		}

		Controller controller = new Controler(scenario);

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(CrossingPenalty.class).toInstance(link -> 0.0);
			}
		});

		return controller;
	}

	private void enableFreeflow(Controller controller) {
		controller.getConfig().qsim().setStorageCapFactor(1e9);
		controller.getConfig().qsim().setFlowCapFactor(1e9);
		VDFConfigGroup.getOrCreate(controller.getConfig()).setCapacityFactor(1e9);
	}

	private void enableVdf(Controller controller, boolean useEngine) {
		Config config = controller.getConfig();

		config.qsim().setStorageCapFactor(1e9);
		config.qsim().setFlowCapFactor(1e9);

		VDFConfigGroup.getOrCreate(config).setCapacityFactor(VDF_CAPACITY_FACTOR);
		controller.addOverridingModule(new VDFModule());
		controller.addOverridingQSimModule(new VDFQSimModule());

		controller.addOverridingQSimModule(new AbstractQSimModule() {
			@Override
			protected void configureQSim() {
				addLinkSpeedCalculatorBinding().to(VDFLinkSpeedCalculator.class);
			}
		});

		if (useEngine) {
			Set<String> mainModes = new HashSet<>(config.qsim().getMainModes());
			mainModes.remove(TransportMode.car);
			config.qsim().setMainModes(mainModes);

			VDFEngineConfigGroup.getOrCreate(config);
			controller.addOverridingModule(new VDFEngineModule());
			controller.configureQSimComponents(VDFEngineModule::configureQSim);
		}
	}

	private void enableDrt(Controller controller, int startLink, boolean useEngine) {
		controller.getConfig().qsim().setSimStarttimeInterpretation(StarttimeInterpretation.onlyUseStarttime);

		DvrpConfigGroup dvrpConfig = new DvrpConfigGroup();
		controller.getConfig().addModule(dvrpConfig);

		MultiModeDrtConfigGroup drtConfig = new MultiModeDrtConfigGroup();
		controller.getConfig().addModule(drtConfig);

		DrtConfigGroup modeConfig = new DrtConfigGroup();
		modeConfig.setMode("drt");
		modeConfig.setStopDuration(60.0);

		drtConfig.addDrtConfigGroup(modeConfig);

		var constraints = modeConfig.addOrGetDrtOptimizationConstraintsParams()
				.addOrGetDefaultDrtOptimizationConstraintsSet();
		constraints.setMaxWaitTime(3600.0);
		constraints.setMaxTravelTimeAlpha(2.0);
		constraints.setMaxTravelTimeBeta(3600.0);

		ExtensiveInsertionSearchParams searchParams = new ExtensiveInsertionSearchParams();
		modeConfig.addParameterSet(searchParams);

		controller.getScenario().getPopulation()
				.getFactory()
				.getRouteFactories()
				.setRouteFactory(DrtRoute.class, new DrtRouteFactory());

		DrtConfigs.adjustMultiModeDrtConfig(drtConfig, controller.getConfig().scoring(),
				controller.getConfig().routing());

		ModeParams modeParams = new ModeParams("drt");
		controller.getConfig().scoring().addModeParams(modeParams);

		controller.addOverridingModule(new DvrpModule());
		controller.addOverridingModule(new MultiModeDrtModule());
		controller.configureQSimComponents(components -> {
			if (useEngine) {
				VDFEngineModule.configureQSim(components);
			}

			DvrpQSimComponents.activateAllModes(drtConfig).configure(components);
		});

		var vehicleSpecification = ImmutableDvrpVehicleSpecification.newBuilder()
				.id(Id.create("veh", DvrpVehicle.class)) //
				.capacity(4) //
				.serviceBeginTime(0.0) //
				.serviceEndTime(24.0 * 3600.0) //
				.startLinkId(Id.createLinkId("f" + startLink))
				.build();

		var fleetSpecification = new FleetSpecificationImpl();
		fleetSpecification.addVehicleSpecification(vehicleSpecification);

		controller.addOverridingModule(new AbstractDvrpModeModule("drt") {
			@Override
			public void install() {
				bindModal(FleetSpecification.class).toInstance(fleetSpecification);
			}
		});
	}

	private void addTrip(Controller controller, double departureTime, int startLinkIndex, int endLinkIndex) {
		addTrip(controller, departureTime, startLinkIndex, endLinkIndex, TransportMode.car);
	}

	private void addTrip(Controller controller, double departureTime, int startLinkIndex, int endLinkIndex,
			String mode) {
		Population population = controller.getScenario().getPopulation();
		PopulationFactory factory = population.getFactory();

		Person person = factory.createPerson(Id.createPersonId(population.getPersons().size()));
		population.addPerson(person);

		Plan plan = factory.createPlan();
		person.addPlan(plan);

		Activity start = factory.createActivityFromLinkId("start", Id.createLinkId("f" + startLinkIndex));
		start.setEndTime(departureTime);
		start.setCoord(controller.getScenario().getNetwork().getLinks().get(start.getLinkId()).getCoord());
		plan.addActivity(start);

		Leg leg = factory.createLeg(mode);
		plan.addLeg(leg);

		Activity end = factory.createActivityFromLinkId("end", Id.createLinkId("f" + endLinkIndex));
		end.setCoord(controller.getScenario().getNetwork().getLinks().get(end.getLinkId()).getCoord());
		plan.addActivity(end);
	}

	private class Analysis implements PersonArrivalEventHandler, PassengerDroppedOffEventHandler {
		private final IdMap<Person, Double> arrivalTimes = new IdMap<>(Person.class);
		private final IdMap<Person, Double> dropoffTimes = new IdMap<>(Person.class);

		@Override
		public void reset(int iteration) {
			arrivalTimes.clear();
		}

		@Override
		public void handleEvent(PersonArrivalEvent event) {
			if (event.getLegMode().equals(TransportMode.car) || event.getLegMode().equals(TransportMode.pt)
					|| event.getLegMode().equals("drt")) {
				arrivalTimes.put(event.getPersonId(), event.getTime());
			}
		}

		@Override
		public void handleEvent(PassengerDroppedOffEvent event) {
			if (event.getMode().equals("drt")) {
				dropoffTimes.put(event.getPersonId(), event.getTime());
			}
		}

		public Double arrivalTime(int personIndex) {
			return arrivalTimes.get(Id.createPersonId(personIndex));
		}

		public Double dropoffTime(int personIndex) {
			return dropoffTimes.get(Id.createPersonId(personIndex));
		}
	}

	private Analysis prepareAnalysis(Controller controller) {
		Analysis instance = new Analysis();

		controller.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addEventHandlerBinding().toInstance(instance);
			}
		});

		return instance;
	}

	@Test
	public void testBaselineFreeflowOne() {
		Controller controller = prepareScenario();
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTrip(controller, 3600.0, 2, 16);

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
	}

	@Test
	public void testBaselineFreeflow20() {
		Controller controller = prepareScenario();
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5020.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5025.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5030.0, analysis.arrivalTime(15), 1e-3);
	}

	@Test
	public void testBaselineCongested20() {
		Controller controller = prepareScenario();

		Analysis analysis = prepareAnalysis(controller);

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5375.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5735.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(6095.0, analysis.arrivalTime(15), 1e-3);
	}

	@Test
	public void testVDFFreeflowOne() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTrip(controller, 3600.0, 2, 16);

		controller.run();

		// equals the qsim baseline!
		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
	}

	@Test
	public void testVDFFreeflow20() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		controller.run();

		// equals the qsim baseline!
		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5020.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5025.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5030.0, analysis.arrivalTime(15), 1e-3);
	}

	@Test
	public void testVDFCongested20() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);

		Analysis analysis = prepareAnalysis(controller);

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		controller.run();

		// different to qsim baseline as dynamics are different!
		assertEquals(5673.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5678.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5683.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5688.0, analysis.arrivalTime(15), 1e-3);
	}

	@Test
	public void testEngineFreeflowOne() {
		for (boolean useDynamic : Arrays.asList(true, false)) {
			for (boolean generateEvents : Arrays.asList(true, false)) {
				Controller controller = prepareScenario();
				enableVdf(controller, true);
				enableFreeflow(controller);

				VDFEngineConfigGroup.getOrCreate(controller.getConfig())
						.setGenerateNetworkEventsInterval(generateEvents ? 1 : 0);

				if (useDynamic) {
					VDFEngineConfigGroup.getOrCreate(controller.getConfig()).setDynamicModes(Set.of(TransportMode.car));
				}

				Analysis analysis = prepareAnalysis(controller);

				addTrip(controller, 3600.0, 2, 16);

				controller.run();

				// equals the qsim baseline!
				assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
			}
		}
	}

	@Test
	public void testEngineFreeflow20() {
		for (boolean useDynamic : Arrays.asList(true, false)) {
			for (boolean generateEvents : Arrays.asList(true, false)) {
				Controller controller = prepareScenario();
				enableVdf(controller, true);
				enableFreeflow(controller);

				VDFEngineConfigGroup.getOrCreate(controller.getConfig())
						.setGenerateNetworkEventsInterval(generateEvents ? 1 : 0);

				if (useDynamic) {
					VDFEngineConfigGroup.getOrCreate(controller.getConfig()).setDynamicModes(Set.of(TransportMode.car));
				}

				Analysis analysis = prepareAnalysis(controller);

				for (int k = 0; k < 20; k++) {
					addTrip(controller, 3600.0 + k, 2, 16);
				}

				controller.run();

				// equals the qsim baseline!
				assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
				assertEquals(5020.0, analysis.arrivalTime(5), 1e-3);
				assertEquals(5025.0, analysis.arrivalTime(10), 1e-3);
				assertEquals(5030.0, analysis.arrivalTime(15), 1e-3);
			}
		}
	}

	@Test
	public void testEngineCongested20() {
		for (boolean useDynamic : Arrays.asList(true)) {
			for (boolean generateEvents : Arrays.asList(true)) {
				Controller controller = prepareScenario();
				enableVdf(controller, true);

				VDFEngineConfigGroup.getOrCreate(controller.getConfig())
						.setGenerateNetworkEventsInterval(generateEvents ? 1 : 0);

				if (useDynamic) {
					VDFEngineConfigGroup.getOrCreate(controller.getConfig()).setDynamicModes(Set.of(TransportMode.car));
				}

				Analysis analysis = prepareAnalysis(controller);

				for (int k = 0; k < 20; k++) {
					addTrip(controller, 3600.0 + k, 2, 16);
				}

				controller.run();

				// equals vdf without engine
				assertEquals(5673.0, analysis.arrivalTime(0), 1e-3);
				assertEquals(5678.0, analysis.arrivalTime(5), 1e-3);
				assertEquals(5683.0, analysis.arrivalTime(10), 1e-3);
				assertEquals(5688.0, analysis.arrivalTime(15), 1e-3);
			}
		}
	}

	private void addTransitLine(Controller controller, double departureTime, int[] stopLinks) {
		controller.getConfig().transit().setUseTransit(true);

		Vehicles vehicles = controller.getScenario().getTransitVehicles();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();

		Id<VehicleType> vehicleTypeId = Id.create("pt", VehicleType.class);
		VehicleType vehicleType = vehicles.getVehicleTypes().get(vehicleTypeId);

		if (vehicleType == null) {
			vehicleType = vehiclesFactory.createVehicleType(vehicleTypeId);
			vehicles.addVehicleType(vehicleType);

			vehicleType.getCapacity().setSeats(4);
			vehicleType.setNetworkMode(TransportMode.car);
		}

		Id<Vehicle> vehicleId = Id.createVehicleId("pt" + vehicles.getVehicles().size());
		Vehicle vehicle = vehiclesFactory.createVehicle(vehicleId, vehicleType);
		vehicles.addVehicle(vehicle);

		TransitSchedule schedule = controller.getScenario().getTransitSchedule();
		int lineIndex = schedule.getTransitLines().size();

		TransitScheduleFactory scheduleFactory = schedule.getFactory();

		Id<TransitLine> transitLineId = Id.create(lineIndex, TransitLine.class);

		TransitLine transitLine = scheduleFactory.createTransitLine(transitLineId);
		schedule.addTransitLine(transitLine);

		Id<TransitRoute> transitRouteId = Id.create(lineIndex, TransitRoute.class);

		LinkNetworkRouteFactory routeFactory = new LinkNetworkRouteFactory();

		Id<Link> startLinkId = Id.createLinkId("f" + stopLinks[0]);

		List<Id<Link>> routeLinks = new ArrayList<>(stopLinks.length - 2);
		for (int i = stopLinks[0] + 1; i < stopLinks[stopLinks.length - 1]; i++) {
			routeLinks.add(Id.createLinkId("f" + i));
		}

		Id<Link> endLinkId = Id.createLinkId("f" + stopLinks[stopLinks.length - 1]);

		NetworkRoute networkRoute = (NetworkRoute) routeFactory.createRoute(startLinkId, endLinkId);
		networkRoute.setLinkIds(startLinkId, routeLinks, endLinkId);

		List<TransitRouteStop> stops = new LinkedList<>();

		for (int stopLinkIndex : stopLinks) {
			Link stopLink = controller.getScenario().getNetwork().getLinks().get(Id.createLinkId("f" + stopLinkIndex));
			TransitStopFacility stopFacility = scheduleFactory.createTransitStopFacility(
					Id.create(lineIndex + "::" + stopLinkIndex, TransitStopFacility.class),
					stopLink.getCoord(), false);
			stopFacility.setLinkId(stopLink.getId());
			schedule.addStopFacility(stopFacility);

			TransitRouteStop stop = scheduleFactory.createTransitRouteStop(stopFacility, OptionalTime.zeroSeconds(),
					OptionalTime.undefined());

			stops.add(stop);
		}

		TransitRoute transitRoute = scheduleFactory.createTransitRoute(transitRouteId, networkRoute, stops, "pt");
		transitLine.addRoute(transitRoute);

		Id<Departure> departureId = Id.create(lineIndex, Departure.class);
		Departure departure = scheduleFactory.createDeparture(departureId, departureTime);
		departure.setVehicleId(vehicleId);
		transitRoute.addDeparture(departure);
	}

	@Test
	public void testTransitBaselineFreeflowTwo() {
		Controller controller = prepareScenario();
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 4, 8, 16 });
		addTrip(controller, 3600.0, 2, 16, "pt");
		addTrip(controller, 3600.0, 4, 8, "pt");

		controller.run();

		assertEquals(5424.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(4613.0, analysis.arrivalTime(1), 1e-3);
	}

	@Test
	public void testTransitBaselineFreeflow20() {
		Controller controller = prepareScenario();
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 16 });

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		addTrip(controller, 3600.0, 2, 16, "pt");

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5020.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5025.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5030.0, analysis.arrivalTime(15), 1e-3);
		assertEquals(5418.0, analysis.arrivalTime(20), 1e-3);
	}

	@Test
	public void testTransitBaselineCongested20() {
		Controller controller = prepareScenario();

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 16 });

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		addTrip(controller, 3600.0, 2, 16, "pt");

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5375.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5735.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(6095.0, analysis.arrivalTime(15), 1e-3);
		assertEquals(6459.0, analysis.arrivalTime(20), 1e-3);
	}

	@Test
	public void testTransitVDFFreeflowOne() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 16 });
		addTrip(controller, 3600.0, 2, 16, "pt");

		controller.run();

		assertEquals(5418.0, analysis.arrivalTime(0), 1e-3);
	}

	@Test
	public void testTransitVDFFreeflow20() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);
		enableFreeflow(controller);

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 16 });

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		addTrip(controller, 3600.0, 2, 16, "pt");

		controller.run();

		assertEquals(5015.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5020.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5025.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5030.0, analysis.arrivalTime(15), 1e-3);
		assertEquals(5418.0, analysis.arrivalTime(20), 1e-3);
	}

	@Test
	public void testTransitVDFCongested20() {
		Controller controller = prepareScenario();
		enableVdf(controller, false);

		Analysis analysis = prepareAnalysis(controller);

		addTransitLine(controller, 4000.0, new int[] { 2, 16 });

		for (int k = 0; k < 20; k++) {
			addTrip(controller, 3600.0 + k, 2, 16);
		}

		addTrip(controller, 3600.0, 2, 16, "pt");

		controller.run();

		assertEquals(5813.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(5818.0, analysis.arrivalTime(5), 1e-3);
		assertEquals(5823.0, analysis.arrivalTime(10), 1e-3);
		assertEquals(5828.0, analysis.arrivalTime(15), 1e-3);
		assertEquals(6216.0, analysis.arrivalTime(20), 1e-3);
	}

	@Test
	public void testTransitEngineFreeflowTwo() {
		for (boolean useDynamic : Arrays.asList(true, false)) {
			for (boolean generateEvents : Arrays.asList(true, false)) {
				Controller controller = prepareScenario();
				enableVdf(controller, true);
				enableFreeflow(controller);

				VDFEngineConfigGroup.getOrCreate(controller.getConfig())
						.setGenerateNetworkEventsInterval(generateEvents ? 1 : 0);

				if (useDynamic) {
					VDFEngineConfigGroup.getOrCreate(controller.getConfig()).setDynamicModes(Set.of(TransportMode.car));
				}

				Analysis analysis = prepareAnalysis(controller);

				addTransitLine(controller, 4000.0, new int[] { 2, 4, 8, 16 });
				addTrip(controller, 3600.0, 2, 16, "pt");
				addTrip(controller, 3600.0, 4, 8, "pt");

				controller.run();

				// equals the qsim baseline!
				assertEquals(5424.0, analysis.arrivalTime(0), 1e-3);
				assertEquals(4613.0, analysis.arrivalTime(1), 1e-3);
			}
		}
	}

	@Test
	public void testDrtFreeflow() {
		Controller controller = prepareScenario();
		enableFreeflow(controller);
		enableDrt(controller, 3, false);

		Analysis analysis = prepareAnalysis(controller);

		addTrip(controller, 3600.0, 2, 16, "drt");
		addTrip(controller, 4200.0, 2, 16, "drt");

		controller.run();

		assertEquals(7029.0, analysis.arrivalTime(0), 1e-3);
		assertEquals(7029.0, analysis.dropoffTime(0), 1e-3);

		assertEquals(7029.0, analysis.arrivalTime(1), 1e-3);
		assertEquals(7029.0, analysis.dropoffTime(1), 1e-3);
	}

	@Test
	public void testDrtFreeflowWithVdfEngine() {
		for (boolean generateEvents : new boolean[] { true, false }) {
			Controller controller = prepareScenario();
			enableFreeflow(controller);
			enableVdf(controller, true);
			enableDrt(controller, 3, true);

			VDFEngineConfigGroup.getOrCreate(controller.getConfig())
					.setGenerateNetworkEventsInterval(generateEvents ? 1 : 0);

			Analysis analysis = prepareAnalysis(controller);

			addTrip(controller, 3600.0, 2, 16, "drt");
			addTrip(controller, 4200.0, 2, 16, "drt");

			controller.run();

			assertEquals(7029.0, analysis.arrivalTime(0), 1e-3);
			assertEquals(7029.0, analysis.dropoffTime(0), 1e-3);

			assertEquals(7029.0, analysis.arrivalTime(1), 1e-3);
			assertEquals(7029.0, analysis.dropoffTime(1), 1e-3);
		}
	}
}
