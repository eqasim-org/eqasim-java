package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.estimators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.mode_choice.constraints.IntermodalVehicleTourConstraint;
import org.eqasim.switzerland.ch_cmdp.config.SwissIntermodalAccessEgressConfigGroup;
import org.eqasim.switzerland.ch_cmdp.routing.IntermodalVehicleRoutingAttributes;
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
import org.matsim.contribs.discrete_mode_choice.components.estimators.CumulativeTourEstimator;
import org.matsim.contribs.discrete_mode_choice.components.filters.TourLengthFilter;
import org.matsim.contribs.discrete_mode_choice.components.tour_finder.ActivityTourFinder;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceModel.NoFeasibleChoiceException;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.model.mode_chain.DefaultModeChainGenerator;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourCandidate;
import org.matsim.contribs.discrete_mode_choice.model.tour_based.TourBasedModel;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.TripEstimator;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.DefaultRoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.RoutedTripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.trip_based.candidates.TripCandidate;
import org.matsim.contribs.discrete_mode_choice.model.utilities.MaximumSelector;
import org.matsim.contribs.discrete_mode_choice.modules.config.DiscreteModeChoiceConfigGroup;
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
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.utils.objectattributes.attributable.AttributesImpl;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.config.SwissRailRaptorConfigGroup.IntermodalAccessEgressParameterSet;
import ch.sbb.matsim.routing.pt.raptor.CapturingSwissHomeActivityRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorIntermodalAccessEgress;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder.Direction;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class TestIntermodalVehicleContinuityTripEstimator {
	@Test
	public void testRequiresEgressAtParkedVehicleStopWhenReturningHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);

		DiscreteModeChoiceTrip returnTrip = createTrip(createActivity("work", "work"), createActivity("home", "home"));
		estimator.estimateTrip(null, TransportMode.pt, returnTrip,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null))));

		assertEquals(TransportMode.bike, delegate.requiredEgressMode);
		assertEquals("stop_home", delegate.requiredEgressStopId);
		assertEquals(TransportMode.bike, delegate.forbiddenAccessMode);
		assertNull(returnTrip.getTripAttributes()
				.getAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE));
		assertNull(returnTrip.getTripAttributes()
				.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE));
		assertNull(returnTrip.getTripAttributes()
				.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID));
	}

	@Test
	public void testRequiresCarEgressAtParkedVehicleStopWhenReturningHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.car), config);

		DiscreteModeChoiceTrip returnTrip = createTrip(createActivity("work", "work"), createActivity("home", "home"));
		estimator.estimateTrip(null, TransportMode.pt, returnTrip,
				List.of(createCandidate(createPtTrip(TransportMode.car, "stop_home", null))));

		assertEquals(TransportMode.car, delegate.requiredEgressMode);
		assertEquals("stop_home", delegate.requiredEgressStopId);
		assertEquals(TransportMode.car, delegate.forbiddenAccessMode);
	}

	@Test
	public void testDoesNotRequireEgressBeforeReturningHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);

		DiscreteModeChoiceTrip intermediateTrip = createTrip(createActivity("work", "work"),
				createActivity("shop", "shop"));
		estimator.estimateTrip(null, TransportMode.pt, intermediateTrip,
				List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_home", null))));

		assertNull(delegate.requiredEgressMode);
		assertNull(delegate.requiredEgressStopId);
		assertNull(delegate.forbiddenAccessMode);
	}

	@Test
	public void testDoesNotRequireEgressWhenNoVehicleWasParkedAtPtStop() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);

		DiscreteModeChoiceTrip returnTrip = createTrip(createActivity("work", "work"), createActivity("home", "home"));
		estimator.estimateTrip(null, TransportMode.pt, returnTrip,
				List.of(createCandidate(createPtTrip(null, "stop_home", null))));

		assertNull(delegate.requiredEgressMode);
		assertNull(delegate.requiredEgressStopId);
		assertNull(delegate.forbiddenAccessMode);
	}

	@Test
	public void testCachesUnconstrainedPtRoute() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);

		DiscreteModeChoiceTrip returnTrip = createTrip(createActivity("work", "work"), createActivity("home", "home"));
		estimator.estimateTrip(null, TransportMode.pt, returnTrip, List.of());
		estimator.estimateTrip(null, TransportMode.pt, returnTrip, List.of());

		assertEquals(1, delegate.callCount);
	}

	@Test
	public void testSeparatesPtCacheByRequiredEgressStop() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTripEstimator estimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);

		DiscreteModeChoiceTrip returnTrip = createTrip(createActivity("work", "work"), createActivity("home", "home"));
		List<TripCandidate> previousAtStopA = List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_a", null)));
		List<TripCandidate> previousAtStopB = List.of(createCandidate(createPtTrip(TransportMode.bike, "stop_b", null)));

		estimator.estimateTrip(null, TransportMode.pt, returnTrip, previousAtStopA);
		estimator.estimateTrip(null, TransportMode.pt, returnTrip, previousAtStopB);
		estimator.estimateTrip(null, TransportMode.pt, returnTrip, previousAtStopA);

		assertEquals(2, delegate.callCount);
	}

	@Test
	public void testTourEstimatorForbidsBikeAccessWhenRemainingChainCannotReturnBikeHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTourEstimator estimator = new IntermodalVehicleContinuityTourEstimator(delegate,
				TimeInterpretation.create(ConfigUtils.createConfig()), config);
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		estimator.estimateTour(null, List.of(TransportMode.pt, TransportMode.walk), tour, List.of());

		assertEquals(TransportMode.bike, delegate.forbiddenAccessModes.get(0));
	}

	@Test
	public void testTourEstimatorForbidsConfiguredVehicleAccessModesWhenRemainingChainCannotReturnThemHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		config.setRestrictedIntermodalAccessEgressModes(TransportMode.bike + "," + TransportMode.car);
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTourEstimator estimator = new IntermodalVehicleContinuityTourEstimator(delegate,
				TimeInterpretation.create(ConfigUtils.createConfig()), config);
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		estimator.estimateTour(null, List.of(TransportMode.pt, TransportMode.walk), tour, List.of());

		assertEquals(TransportMode.bike + "," + TransportMode.car, delegate.forbiddenAccessModes.get(0));
	}

	@Test
	public void testTourEstimatorAllowsBikeAccessWhenRemainingChainCanReturnBikeHome() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		CapturingEstimator delegate = new CapturingEstimator();
		IntermodalVehicleContinuityTourEstimator estimator = new IntermodalVehicleContinuityTourEstimator(delegate,
				TimeInterpretation.create(ConfigUtils.createConfig()), config);
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();

		estimator.estimateTour(null, List.of(TransportMode.pt, TransportMode.pt), tour, List.of());

		assertNull(delegate.forbiddenAccessModes.get(0));
	}

	@Test
	public void testTourEstimatorReroutesEarlierPtAccessWhenVehicleReturnIsInfeasible() {
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour();
		RetryingEstimator delegate = new RetryingEstimator(tour.get(0), tour.get(1));
		IntermodalVehicleContinuityTripEstimator tripEstimator = new IntermodalVehicleContinuityTripEstimator(delegate,
				List.of(TransportMode.bike), config);
		IntermodalVehicleContinuityTourEstimator tourEstimator = new IntermodalVehicleContinuityTourEstimator(
				tripEstimator, TimeInterpretation.create(ConfigUtils.createConfig()), config);

		TourCandidate candidate = tourEstimator.estimateTour(null, List.of(TransportMode.pt, TransportMode.pt), tour,
				List.of());

		assertEquals(2, delegate.outboundCalls);
		assertEquals(2, delegate.returnCalls);
		assertEquals(TransportMode.bike, delegate.outboundForbiddenAccessModes.get(1));
		assertEquals("walk->walk", getAccessMode(getRoute(candidate.getTripCandidates().get(0))) + "->"
				+ getEgressMode(getRoute(candidate.getTripCandidates().get(0))));
		assertEquals("walk->walk", getAccessMode(getRoute(candidate.getTripCandidates().get(1))) + "->"
				+ getEgressMode(getRoute(candidate.getTripCandidates().get(1))));
	}

	@Test
	public void testTourLevelDmcRoutesPtReturnToParkedIntermodalVehicle() throws NoFeasibleChoiceException {
		Scenario scenario = createIntermodalTransitScenario();
		Person person = createHomeWorkHomePerson(scenario);
		RoutingFixture routing = createRouter(scenario);
		SwissIntermodalAccessEgressConfigGroup config = createConfig();
		config.setRestrictVehicleToHomeActivity(true);

		TripEstimator routingEstimator = new RoutingTripEstimator(scenario, routing.router);
		IntermodalVehicleContinuityTripEstimator continuityEstimator = new IntermodalVehicleContinuityTripEstimator(
				routingEstimator, List.of(TransportMode.bike), config);
		DiscreteModeChoiceModel model = new TourBasedModel(
				new CumulativeTourEstimator(continuityEstimator, TimeInterpretation.create(ConfigUtils.createConfig())),
				new StaticModeAvailability(), (p, trips, modes) -> new IntermodalVehicleTourConstraint(
						List.of(TransportMode.bike), Id.createLinkId("home_work")),
				new ActivityTourFinder(List.of("home")), (p, tour) -> true, new MaximumSelector.Factory(),
				new DefaultModeChainGenerator.Factory(), DiscreteModeChoiceModel.FallbackBehaviour.EXCEPTION,
				TimeInterpretation.create(ConfigUtils.createConfig()));

		List<DiscreteModeChoiceTrip> tour = createHomeWorkHomeTour(person);
		List<TripCandidate> candidates = model.chooseModes(person, tour, new Random(0L));

		assertEquals(List.of(TransportMode.pt, TransportMode.pt),
				candidates.stream().map(TripCandidate::getMode).toList());
		assertEquals("bike->walk", getAccessMode(getRoute(candidates.get(0))) + "->"
				+ getEgressMode(getRoute(candidates.get(0))));
		assertEquals("walk->bike", getAccessMode(getRoute(candidates.get(1))) + "->"
				+ getEgressMode(getRoute(candidates.get(1))));
		assertEquals(getFirstTransitRoute(getRoute(candidates.get(0))).getAccessStopId(),
				getLastTransitRoute(getRoute(candidates.get(1))).getEgressStopId());
		assertEquals(TransportMode.bike, routing.stopFinder.getRequiredEgressModes().get("home_outbound"));
		assertTrue(routing.stopFinder.getForbiddenAccessModes().contains(TransportMode.bike));
	}

	@Test(expected = NoFeasibleChoiceException.class)
	public void testInitialChoiceFallbackDoesNotBypassIntermodalVehicleConstraint() throws NoFeasibleChoiceException {
		Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
		TripEstimator tripEstimator = (p, mode, trip, previousTrips) -> TransportMode.pt.equals(mode)
				? createCandidate(createPtTrip(TransportMode.bike, "stop_home", null))
				: createCandidate(mode, List.of(createLeg(mode)));
		DiscreteModeChoiceModel model = new TourBasedModel(
				new CumulativeTourEstimator(tripEstimator, TimeInterpretation.create(ConfigUtils.createConfig())),
				(p, trips) -> List.of(), (p, trips, modes) -> new IntermodalVehicleTourConstraint(
						List.of(TransportMode.bike), Id.createLinkId("home")),
				new ActivityTourFinder(List.of("home")), (p, tour) -> true, new MaximumSelector.Factory(),
				new DefaultModeChainGenerator.Factory(), DiscreteModeChoiceModel.FallbackBehaviour.INITIAL_CHOICE,
				TimeInterpretation.create(ConfigUtils.createConfig()));

		model.chooseModes(person, createInitialPtWalkHomeWorkHomeTour(), new Random(0L));
	}

	@Test
	public void testTourLengthFilterUsesConfiguredMaximumLength() {

		TourLengthFilter filter = new TourLengthFilter(10);

		assertTrue(filter.filter(null, createTrips(7)));
	}

	static private SwissIntermodalAccessEgressConfigGroup createConfig() {
		SwissIntermodalAccessEgressConfigGroup config = new SwissIntermodalAccessEgressConfigGroup();
		config.setEnforceIntermodalVehicleContinuityDuringRouting(true);
		config.setIntermodalVehicleContinuityHomeActivityType("home");
		return config;
	}

	static private Scenario createIntermodalTransitScenario() {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		createNetwork(scenario.getNetwork());
		createSchedule(scenario);
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

		scenario.getTransitSchedule().addStopFacility(homeOutbound);
		scenario.getTransitSchedule().addStopFacility(workOutbound);
		scenario.getTransitSchedule().addStopFacility(workInbound);

		TransitLine line = factory.createTransitLine(Id.create("bus", TransitLine.class));
		line.addRoute(createTransitRoute(factory, "outbound", Id.createLinkId("home_work"), homeOutbound, workOutbound));
		line.addRoute(createTransitRoute(factory, "inbound", Id.createLinkId("work_home"), workInbound, homeOutbound));
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

	static private Person createHomeWorkHomePerson(Scenario scenario) {
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
		Plan plan = scenario.getPopulation().getFactory().createPlan();

		Activity home = createActivity("home", Id.createLinkId("home_work"), new Coord(0.0, 0.0));
		home.setEndTime(8.0 * 3600.0);
		Activity work = createActivity("work", Id.createLinkId("work_home"), new Coord(10000.0, 0.0));
		work.setEndTime(17.0 * 3600.0);
		Activity homeAgain = createActivity("home", Id.createLinkId("home_work"), new Coord(0.0, 0.0));

		plan.addActivity(home);
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(work);
		plan.addLeg(PopulationUtils.createLeg(TransportMode.pt));
		plan.addActivity(homeAgain);
		person.addPlan(plan);
		scenario.getPopulation().addPerson(person);
		return person;
	}

	static private Activity createActivity(String type, Id<Link> linkId, Coord coord) {
		Activity activity = PopulationUtils.createActivityFromLinkId(type, linkId);
		activity.setCoord(coord);
		return activity;
	}

	static private List<DiscreteModeChoiceTrip> createHomeWorkHomeTour(Person person) {
		Plan plan = person.getSelectedPlan();
		Activity home = (Activity) plan.getPlanElements().get(0);
		Activity work = (Activity) plan.getPlanElements().get(2);
		Activity homeAgain = (Activity) plan.getPlanElements().get(4);

		return List.of(new DiscreteModeChoiceTrip(home, work, TransportMode.pt,
				List.of(PopulationUtils.createLeg(TransportMode.pt)), 0, 0, 0, new AttributesImpl()),
				new DiscreteModeChoiceTrip(work, homeAgain, TransportMode.pt,
						List.of(PopulationUtils.createLeg(TransportMode.pt)), 0, 1, 1, new AttributesImpl()));
	}

	static private List<DiscreteModeChoiceTrip> createHomeWorkHomeTour() {
		DiscreteModeChoiceTrip outbound = createTrip(createActivity("home", "home"), createActivity("work", "work"));
		outbound.setDepartureTime(0.0);
		return List.of(outbound, createTrip(createActivity("work", "work"), createActivity("home", "home")));
	}

	static private List<DiscreteModeChoiceTrip> createInitialPtWalkHomeWorkHomeTour() {
		Activity home = createActivity("home", "home");
		Activity work = createActivity("work", "work");
		Activity homeAgain = createActivity("home", "home");
		DiscreteModeChoiceTrip outbound = new DiscreteModeChoiceTrip(home, work, TransportMode.pt,
				List.of(PopulationUtils.createLeg(TransportMode.pt)), 0, 0, 0, new AttributesImpl());
		outbound.setDepartureTime(0.0);
		return List.of(outbound, new DiscreteModeChoiceTrip(work, homeAgain, TransportMode.walk,
				List.of(PopulationUtils.createLeg(TransportMode.walk)), 0, 1, 1, new AttributesImpl()));
	}

	static private List<DiscreteModeChoiceTrip> createTrips(int count) {
		List<DiscreteModeChoiceTrip> trips = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			trips.add(createTrip(createActivity("home", "home_" + i), createActivity("work", "work_" + i)));
		}
		return trips;
	}

	static private RoutingFixture createRouter(Scenario scenario) {
		SwissRailRaptorConfigGroup raptorConfig = new SwissRailRaptorConfigGroup();
		raptorConfig.setUseIntermodalAccessEgress(true);
		raptorConfig.addIntermodalAccessEgress(createIntermodalMode(TransportMode.walk));
		raptorConfig.addIntermodalAccessEgress(createIntermodalMode(TransportMode.bike));

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
		DefaultRaptorStopFinder delegate = new DefaultRaptorStopFinder(new DeterministicIntermodalAccessEgress(),
				Map.of(TransportMode.walk, walk, TransportMode.bike, bike));
		SwissIntermodalAccessEgressConfigGroup accessEgressConfig = new SwissIntermodalAccessEgressConfigGroup();
		accessEgressConfig.setRestrictVehicleToHomeActivity(true);
		CapturingSwissHomeActivityRaptorStopFinder stopFinder = new CapturingSwissHomeActivityRaptorStopFinder(delegate,
				accessEgressConfig);
		SwissRailRaptor router = new SwissRailRaptor.Builder(data, ConfigUtils.createConfig()).with(person -> parameters)
				.with(stopFinder).build();
		return new RoutingFixture(router, stopFinder);
	}

	static private IntermodalAccessEgressParameterSet createIntermodalMode(String mode) {
		return new IntermodalAccessEgressParameterSet().setMode(mode).setInitialSearchRadius(2000.0)
				.setSearchExtensionRadius(100.0).setMaxRadius(2000.0);
	}

	static private DiscreteModeChoiceTrip createTrip(Activity origin, Activity destination) {
		return new DiscreteModeChoiceTrip(origin, destination, TransportMode.pt,
				List.of(PopulationUtils.createLeg(TransportMode.pt)), 0, 0, 0, new AttributesImpl());
	}

	static private Activity createActivity(String type, String linkId) {
		Activity activity = PopulationUtils.createActivityFromLinkId(type, Id.createLinkId(linkId));
		activity.setEndTime(0.0);
		return activity;
	}

	static private DefaultRoutedTripCandidate createCandidate(List<? extends PlanElement> elements) {
		return new DefaultRoutedTripCandidate(0.0, TransportMode.pt, elements, 0.0);
	}

	static private DefaultRoutedTripCandidate createCandidate(String mode, List<? extends PlanElement> elements) {
		return new DefaultRoutedTripCandidate(0.0, mode, elements, 0.0);
	}

	static private List<? extends PlanElement> createPtTrip(String accessMode, String accessStopId,
			String egressMode) {
		Leg pt = PopulationUtils.createLeg(TransportMode.pt);
		Id<TransitStopFacility> accessStop = Id.create(accessStopId, TransitStopFacility.class);
		Id<TransitStopFacility> egressStop = Id.create("work_stop", TransitStopFacility.class);
		pt.setRoute(new DefaultTransitPassengerRoute(Id.createLinkId(accessStop), Id.createLinkId(egressStop),
				accessStop, egressStop, Id.create("line", TransitLine.class),
				Id.create("route", TransitRoute.class)));

		if (accessMode != null) {
			return List.of(createLeg(accessMode), pt, createLeg(TransportMode.walk));
		}

		if (egressMode != null) {
			return List.of(createLeg(TransportMode.walk), pt, createLeg(egressMode));
		}

		return List.of(createLeg(TransportMode.walk), pt, createLeg(TransportMode.walk));
	}

	static private Leg createLeg(String mode) {
		Leg leg = PopulationUtils.createLeg(mode);
		leg.setRoute(RouteUtils.createGenericRouteImpl(Id.create("from_" + mode, Link.class),
				Id.create("to_" + mode, Link.class)));
		return leg;
	}

	static private List<? extends PlanElement> getRoute(TripCandidate candidate) {
		return ((RoutedTripCandidate) candidate).getRoutedPlanElements();
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

	static private TransitPassengerRoute getFirstTransitRoute(List<? extends PlanElement> route) {
		for (PlanElement element : route) {
			if (element instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute transitRoute) {
				return transitRoute;
			}
		}
		throw new IllegalStateException("Route does not contain a transit leg.");
	}

	static private TransitPassengerRoute getLastTransitRoute(List<? extends PlanElement> route) {
		TransitPassengerRoute lastTransitRoute = null;
		for (PlanElement element : route) {
			if (element instanceof Leg leg && leg.getRoute() instanceof TransitPassengerRoute transitRoute) {
				lastTransitRoute = transitRoute;
			}
		}
		if (lastTransitRoute == null) {
			throw new IllegalStateException("Route does not contain a transit leg.");
		}
		return lastTransitRoute;
	}

	static private double getDuration(List<? extends PlanElement> route) {
		double duration = 0.0;
		for (PlanElement element : route) {
			if (element instanceof Leg leg && leg.getTravelTime().isDefined()) {
				duration += leg.getTravelTime().seconds();
			}
		}
		return duration;
	}

	static private Facility createFacility(Scenario scenario, Activity activity) {
		return FacilitiesUtils.wrapLinkAndCoord(scenario.getNetwork().getLinks().get(activity.getLinkId()),
				activity.getCoord());
	}

	static private class CapturingEstimator implements TripEstimator {
		private int callCount;
		private Object forbiddenAccessMode;
		private Object requiredEgressMode;
		private Object requiredEgressStopId;
		private final List<Object> forbiddenAccessModes = new ArrayList<>();

		@Override
		public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
				List<TripCandidate> previousTrips) {
			callCount++;
			forbiddenAccessMode = trip.getTripAttributes()
					.getAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE);
			requiredEgressMode = trip.getTripAttributes()
					.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE);
			requiredEgressStopId = trip.getTripAttributes()
					.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_STOP_ID);
			forbiddenAccessModes.add(forbiddenAccessMode);
			return new DefaultRoutedTripCandidate(0.0, mode, trip.getInitialElements(), 0.0);
		}
	}

	static private class RetryingEstimator implements TripEstimator {
		private final DiscreteModeChoiceTrip outboundTrip;
		private final DiscreteModeChoiceTrip returnTrip;
		private int outboundCalls;
		private int returnCalls;
		private final List<Object> outboundForbiddenAccessModes = new ArrayList<>();

		private RetryingEstimator(DiscreteModeChoiceTrip outboundTrip, DiscreteModeChoiceTrip returnTrip) {
			this.outboundTrip = outboundTrip;
			this.returnTrip = returnTrip;
		}

		@Override
		public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
				List<TripCandidate> previousTrips) {
			if (trip == outboundTrip) {
				outboundCalls++;
				Object forbiddenAccessMode = trip.getTripAttributes()
						.getAttribute(IntermodalVehicleRoutingAttributes.FORBIDDEN_ACCESS_MODE);
				outboundForbiddenAccessModes.add(forbiddenAccessMode);
				if (containsMode(forbiddenAccessMode, TransportMode.bike)) {
					return createCandidate(createPtTrip(null, "stop_home", null));
				}
				return createCandidate(createPtTrip(TransportMode.bike, "stop_home", null));
			}

			if (trip == returnTrip) {
				returnCalls++;
				Object requiredEgressMode = trip.getTripAttributes()
						.getAttribute(IntermodalVehicleRoutingAttributes.REQUIRED_EGRESS_MODE);
				if (TransportMode.bike.equals(requiredEgressMode)) {
					throw new RuntimeException("Bike egress is infeasible in this test fixture.");
				}
				return createCandidate(createPtTrip(null, "stop_home", null));
			}

			throw new IllegalArgumentException("Unexpected trip.");
		}
	}

	static private boolean containsMode(Object value, String mode) {
		if (value == null) {
			return false;
		}

		for (String token : value.toString().split(",")) {
			if (mode.equals(token.trim())) {
				return true;
			}
		}

		return false;
	}

	static private class RoutingTripEstimator implements TripEstimator {
		private final Scenario scenario;
		private final SwissRailRaptor router;

		private RoutingTripEstimator(Scenario scenario, SwissRailRaptor router) {
			this.scenario = scenario;
			this.router = router;
		}

		@Override
		public TripCandidate estimateTrip(Person person, String mode, DiscreteModeChoiceTrip trip,
				List<TripCandidate> previousTrips) {
			if (TransportMode.pt.equals(mode)) {
				Facility from = createFacility(scenario, trip.getOriginActivity());
				Facility to = createFacility(scenario, trip.getDestinationActivity());
				List<? extends PlanElement> route = router.calcRoute(DefaultRoutingRequest.of(from, to,
						trip.getDepartureTime(), person, trip.getTripAttributes()));
				return new DefaultRoutedTripCandidate(10.0, mode, route, getDuration(route));
			}

			Leg leg = PopulationUtils.createLeg(mode);
			leg.setTravelTime(3600.0);
			return new DefaultRoutedTripCandidate(0.0, mode, List.of(leg), 3600.0);
		}
	}

	static private class StaticModeAvailability implements ModeAvailability {
		@Override
		public Collection<String> getAvailableModes(Person person, List<DiscreteModeChoiceTrip> trips) {
			return List.of(TransportMode.walk, TransportMode.bike, TransportMode.pt);
		}
	}

	static private class DeterministicIntermodalAccessEgress implements RaptorIntermodalAccessEgress {
		@Override
		public RIntermodalAccessEgress calcIntermodalAccessEgress(List<? extends PlanElement> legs,
				RaptorParameters params, Person person, Direction direction) {
			double disutility = 0.0;
			double travelTime = 0.0;
			for (PlanElement element : legs) {
				if (element instanceof Leg leg && leg.getTravelTime().isDefined()) {
					travelTime += leg.getTravelTime().seconds();
					disutility += leg.getTravelTime().seconds()
							* -params.getMarginalUtilityOfTravelTime_utl_s(leg.getMode());
				}
			}
			return new RIntermodalAccessEgress(legs, disutility, travelTime, direction);
		}
	}

	static private class BeelineRoutingModule implements RoutingModule {
		private final String mode;
		private final double speed;

		private BeelineRoutingModule(String mode, double speed) {
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

	static private class RoutingFixture {
		private final SwissRailRaptor router;
		private final CapturingSwissHomeActivityRaptorStopFinder stopFinder;

		private RoutingFixture(SwissRailRaptor router, CapturingSwissHomeActivityRaptorStopFinder stopFinder) {
			this.router = router;
			this.stopFinder = stopFinder;
		}
	}
}
