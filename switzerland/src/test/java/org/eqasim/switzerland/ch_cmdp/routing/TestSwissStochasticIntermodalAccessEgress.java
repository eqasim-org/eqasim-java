package org.eqasim.switzerland.ch_cmdp.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress.RIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder.Direction;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.routing.pt.raptor.SwissHomeActivityRaptorStopFinder;

public class TestSwissStochasticIntermodalAccessEgress {
	@Test
	public void testZeroScaleKeepsDeterministicDisutility() {
		SwissStochasticIntermodalAccessEgress calculator = new SwissStochasticIntermodalAccessEgress(0L, 0.0,
				Set.of());

		RIntermodalAccessEgress result = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.walk)),
				createParameters(), createPerson("person"), Direction.ACCESS);

		assertEquals(1.0, result.disutility, 1e-9);
		assertEquals(3600.0, result.travelTime, 1e-9);
	}

	@Test
	public void testUtilityErrorIsStableForSamePersonAndMode() {
		SwissStochasticIntermodalAccessEgress calculator = new SwissStochasticIntermodalAccessEgress(0L, 1.0,
				Set.of());
		Person person = createPerson("person");
		RaptorParameters parameters = createParameters();

		RIntermodalAccessEgress first = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.bike)),
				parameters, person, Direction.ACCESS);
		RIntermodalAccessEgress second = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.bike)),
				parameters, person, Direction.ACCESS);

		assertEquals(first.disutility, second.disutility, 1e-9);
	}

	@Test
	public void testUtilityErrorVariesAcrossPersons() {
		SwissStochasticIntermodalAccessEgress calculator = new SwissStochasticIntermodalAccessEgress(0L, 1.0,
				Set.of());
		RaptorParameters parameters = createParameters();

		RIntermodalAccessEgress first = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.bike)),
				parameters, createPerson("first"), Direction.ACCESS);
		RIntermodalAccessEgress second = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.bike)),
				parameters, createPerson("second"), Direction.ACCESS);

		assertNotEquals(first.disutility, second.disutility, 1e-9);
	}

	@Test
	public void testUtilityErrorCanBeRestrictedToSpecificModes() {
		SwissStochasticIntermodalAccessEgress calculator = new SwissStochasticIntermodalAccessEgress(0L, 1.0,
				Set.of(TransportMode.bike));
		Person person = createPerson("person");
		RaptorParameters parameters = createParameters();

		RIntermodalAccessEgress walk = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.walk)),
				parameters, person, Direction.ACCESS);
		RIntermodalAccessEgress bike = calculator.calcIntermodalAccessEgress(List.of(createLeg(TransportMode.bike)),
				parameters, person, Direction.ACCESS);

		assertEquals(1.0, walk.disutility, 1e-9);
		assertNotEquals(1.0, bike.disutility, 1e-9);
	}

	static private Person createPerson(String id) {
		return PopulationUtils.getFactory().createPerson(Id.createPersonId(id));
	}

	static private Leg createLeg(String mode) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setTravelTime(3600.0);
		return leg;
	}

	static private RaptorParameters createParameters() {
		RaptorParameters parameters = new RaptorParameters(new SwissRailRaptorConfigGroup());
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.walk, -1.0 / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.bike, -1.0 / 3600.0);
		return parameters;
	}

	@Test
	public void testDeterministicAndStochasticIntermodalPtRouteChoices() {
		Scenario scenario = createIntermodalTransitScenario();
		Facility home = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId("home_work")),
				new Coord(0.0, 0.0));
		Facility work = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId("work_home")),
				new Coord(10000.0, 0.0));

		SwissRailRaptor deterministicRouter = createRouter(scenario, 0.0, false);
		SwissRailRaptor stochasticRouter = createRouter(scenario, 1.0, false);
		Map<String, Integer> deterministicChoices = new LinkedHashMap<>();
		Map<String, Integer> stochasticChoices = new LinkedHashMap<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			addChoice(deterministicChoices, deterministicRouter, home, work, 8.0 * 3600.0, person);
			addChoice(deterministicChoices, deterministicRouter, work, home, 17.0 * 3600.0, person);
			addChoice(stochasticChoices, stochasticRouter, home, work, 8.0 * 3600.0, person);
			addChoice(stochasticChoices, stochasticRouter, work, home, 17.0 * 3600.0, person);
		}

		System.out.println("Deterministic intermodal pt route choices: " + deterministicChoices);
		System.out.println("Stochastic intermodal pt route choices: " + stochasticChoices);

		assertEquals(Map.of("bike->bike", 200), deterministicChoices);
		assertEquals(200, stochasticChoices.values().stream().mapToInt(Integer::intValue).sum());
		assertTrue(stochasticChoices.size() > 1);
	}

	@Test
	public void testBikeAccessAndEgressCanBeRestrictedToHomeActivity() {
		Scenario scenario = createIntermodalTransitScenario();
		Facility home = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId("home_work")),
				new Coord(0.0, 0.0));
		Facility work = FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(Id.createLinkId("work_home")),
				new Coord(10000.0, 0.0));
		SwissRailRaptor router = createRouter(scenario, 0.0, true);
		Map<String, Integer> outboundChoices = new LinkedHashMap<>();
		Map<String, Integer> inboundChoices = new LinkedHashMap<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			addChoice(outboundChoices, router, home, work, 8.0 * 3600.0, person);
			addChoice(inboundChoices, router, work, home, 17.0 * 3600.0, person);
		}

		assertEquals(Map.of("bike->walk", 100), outboundChoices);
		assertEquals(Map.of("walk->bike", 100), inboundChoices);
	}

	static private Scenario createIntermodalTransitScenario() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario.getNetwork());
		createSchedule(scenario);
		createPopulation(scenario);
		return scenario;
	}

	static private void createNetwork(Network network) {
		Node home = NetworkUtils.createAndAddNode(network, Id.createNodeId("home"), new Coord(0.0, 0.0));
		Node work = NetworkUtils.createAndAddNode(network, Id.createNodeId("work"), new Coord(10000.0, 0.0));
		createLink(network, Id.createLinkId("home_work"), home, work);
		createLink(network, Id.createLinkId("work_home"), work, home);
	}

	static private void createLink(Network network, Id<Link> id, Node fromNode, Node toNode) {
		Link link = NetworkUtils.createAndAddLink(network, id, fromNode, toNode, 10000.0, 20.0, 3600.0, 1.0);
		link.setAllowedModes(Set.of(TransportMode.car, TransportMode.pt, TransportMode.bike));
	}

	static private void createSchedule(Scenario scenario) {
		TransitScheduleFactory factory = scenario.getTransitSchedule().getFactory();
		TransitStopFacility homeOutbound = createStop(factory, "home_outbound", new Coord(1000.0, 0.0),
				Id.createLinkId("home_work"));
		TransitStopFacility workOutbound = createStop(factory, "work_outbound", new Coord(8500.0, 0.0),
				Id.createLinkId("home_work"));
		TransitStopFacility workInbound = createStop(factory, "work_inbound", new Coord(8500.0, 0.0),
				Id.createLinkId("work_home"));
		TransitStopFacility homeInbound = createStop(factory, "home_inbound", new Coord(1000.0, 0.0),
				Id.createLinkId("work_home"));

		scenario.getTransitSchedule().addStopFacility(homeOutbound);
		scenario.getTransitSchedule().addStopFacility(workOutbound);
		scenario.getTransitSchedule().addStopFacility(workInbound);
		scenario.getTransitSchedule().addStopFacility(homeInbound);

		TransitLine line = factory.createTransitLine(Id.create("bus", TransitLine.class));
		line.addRoute(createTransitRoute(factory, "outbound", Id.createLinkId("home_work"), homeOutbound, workOutbound));
		line.addRoute(createTransitRoute(factory, "inbound", Id.createLinkId("work_home"), workInbound, homeInbound));
		scenario.getTransitSchedule().addTransitLine(line);
	}

	static private TransitStopFacility createStop(TransitScheduleFactory factory, String id, Coord coord,
			Id<Link> linkId) {
		TransitStopFacility stop = factory.createTransitStopFacility(Id.create(id, TransitStopFacility.class), coord,
				false);
		stop.setLinkId(linkId);
		return stop;
	}

	static private TransitRoute createTransitRoute(TransitScheduleFactory factory, String id, Id<Link> linkId,
			TransitStopFacility firstStop, TransitStopFacility secondStop) {
		List<TransitRouteStop> stops = List.of(factory.createTransitRouteStop(firstStop, 0.0, 0.0),
				factory.createTransitRouteStop(secondStop, 600.0, 600.0));
		TransitRoute route = factory.createTransitRoute(Id.create(id, TransitRoute.class),
				RouteUtils.createLinkNetworkRouteImpl(linkId, linkId), stops, "bus");

		for (int i = 0; i < 70; i++) {
			Departure departure = factory.createDeparture(Id.create(id + "_" + i, Departure.class),
					7.0 * 3600.0 + i * 600.0);
			route.addDeparture(departure);
		}

		return route;
	}

	static private void createPopulation(Scenario scenario) {
		for (int i = 0; i < 100; i++) {
			Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person_" + i));
			Plan plan = scenario.getPopulation().getFactory().createPlan();

			Activity home = PopulationUtils.createActivityFromCoord("home", new Coord(0.0, 0.0));
			home.setEndTime(8.0 * 3600.0);
			Activity work = PopulationUtils.createActivityFromCoord("work", new Coord(10000.0, 0.0));
			work.setEndTime(17.0 * 3600.0);
			Activity homeAgain = PopulationUtils.createActivityFromCoord("home", new Coord(0.0, 0.0));

			plan.addActivity(home);
			plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
			plan.addActivity(work);
			plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
			plan.addActivity(homeAgain);
			person.addPlan(plan);
			scenario.getPopulation().addPerson(person);
		}
	}

	static private SwissRailRaptor createRouter(Scenario scenario, double utilityErrorScale, boolean restrictBikeToHome) {
		SwissRailRaptorConfigGroup raptorConfig = new SwissRailRaptorConfigGroup();
		raptorConfig.setUseIntermodalAccessEgress(true);
		raptorConfig.addIntermodalAccessEgress(createIntermodalMode(TransportMode.walk));
		raptorConfig.addIntermodalAccessEgress(createIntermodalMode(TransportMode.bike));
		SwissIntermodalAccessEgressConfigGroup accessEgressConfig = new SwissIntermodalAccessEgressConfigGroup();
		accessEgressConfig.setRestrictBikeToHomeActivity(restrictBikeToHome);

		RaptorParameters parameters = new RaptorParameters(raptorConfig);
		parameters.setBeelineWalkSpeed(1.0);
		parameters.setDirectWalkFactor(1000.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.walk, -1.0 / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.bike, -1.0 / 3600.0);
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.pt, -1.0 / 3600.0);
		parameters.setMarginalUtilityOfWaitingPt_utl_s(-1.0 / 3600.0);

		RaptorStaticConfig staticConfig = new RaptorStaticConfig();
		staticConfig.setBeelineWalkSpeed(1.0);
		staticConfig.setBeelineWalkDistanceFactor(1.0);
		staticConfig.setBeelineWalkConnectionDistance(100.0);

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, staticConfig,
				scenario.getNetwork(), null);
		RoutingModule walk = new BeelineRoutingModule(TransportMode.walk, 1.0);
		RoutingModule bike = new BeelineRoutingModule(TransportMode.bike, 3.0);
		DefaultRaptorStopFinder delegate = new DefaultRaptorStopFinder(new SwissStochasticIntermodalAccessEgress(0L,
				utilityErrorScale, Set.of(TransportMode.walk, TransportMode.bike)),
				Map.of(TransportMode.walk, walk, TransportMode.bike, bike));

		return new SwissRailRaptor.Builder(data, ConfigUtils.createConfig()).with(person -> parameters)
				.with(new SwissHomeActivityRaptorStopFinder(delegate, accessEgressConfig))
				.build();
	}

	static private IntermodalAccessEgressParameterSet createIntermodalMode(String mode) {
		return new IntermodalAccessEgressParameterSet().setMode(mode).setInitialSearchRadius(2000.0)
				.setSearchExtensionRadius(100.0).setMaxRadius(2000.0);
	}

	static private void addChoice(Map<String, Integer> choices, SwissRailRaptor router, Facility from, Facility to,
			double departureTime, Person person) {
		List<? extends PlanElement> route = router
				.calcRoute(DefaultRoutingRequest.withoutAttributes(from, to, departureTime, person));
		assertTrue(route.stream().filter(Leg.class::isInstance).map(Leg.class::cast)
				.anyMatch(leg -> leg.getRoute() instanceof TransitPassengerRoute));
		choices.merge(getAccessMode(route) + "->" + getEgressMode(route), 1, Integer::sum);
	}

	static private String getAccessMode(List<? extends PlanElement> route) {
		for (PlanElement element : route) {
			if (element instanceof Leg leg) {
				if (leg.getRoute() instanceof TransitPassengerRoute) {
					throw new IllegalStateException("Route does not contain an access leg before the pt leg.");
				}
				return leg.getMode();
			}
		}
		throw new IllegalStateException("Route does not contain any legs.");
	}

	static private String getEgressMode(List<? extends PlanElement> route) {
		List<Leg> legsAfterPt = new ArrayList<>();
		boolean foundPt = false;
		for (PlanElement element : route) {
			if (element instanceof Leg leg) {
				if (leg.getRoute() instanceof TransitPassengerRoute) {
					foundPt = true;
					legsAfterPt.clear();
				} else if (foundPt) {
					legsAfterPt.add(leg);
				}
			}
		}
		if (legsAfterPt.isEmpty()) {
			throw new IllegalStateException("Route does not contain an egress leg after the pt leg.");
		}
		return legsAfterPt.get(legsAfterPt.size() - 1).getMode();
	}

	static private class BeelineRoutingModule implements RoutingModule {
		private final String mode;
		private final double speed;

		BeelineRoutingModule(String mode, double speed) {
			this.mode = mode;
			this.speed = speed;
		}

		@Override
		public List<? extends PlanElement> calcRoute(RoutingRequest request) {
			double distance = CoordUtils.calcEuclideanDistance(request.getFromFacility().getCoord(),
					request.getToFacility().getCoord());
			double travelTime = distance / speed;
			Leg leg = PopulationUtils.createLeg(mode);
			leg.setDepartureTime(request.getDepartureTime());
			leg.setTravelTime(travelTime);
			leg.setRoute(RouteUtils.createGenericRouteImpl(request.getFromFacility().getLinkId(),
					request.getToFacility().getLinkId()));
			leg.getRoute().setDistance(distance);
			leg.getRoute().setTravelTime(travelTime);
			return List.of(leg);
		}
	}
}
