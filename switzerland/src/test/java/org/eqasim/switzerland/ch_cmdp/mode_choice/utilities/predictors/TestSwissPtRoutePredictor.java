package org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.variables.SwissPtLegVariables;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Authority;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.NetworkOfDistances;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.ZonalRegistry;
import org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs.Zone;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestSwissPtRoutePredictor {
	private static final Authority LOCAL = new Authority("LOCAL", 2, "ZoneBased");
	private static final Authority OTHER = new Authority("OTHER", 1, "ZoneBased");
	private static final Authority SBB = new Authority("SBB", 1, "DistanceBased");

	@Test
	public void testSimplifyStopIdOfSloids() {
		assertEquals("8503000", SwissPtRoutePredictor.simplifyStopId("8503000:0:3"));
		assertEquals("8503000", SwissPtRoutePredictor.simplifyStopId("ch:1:sloid:3000:2:3"));
		assertEquals("8578189", SwissPtRoutePredictor.simplifyStopId("Parentch:1:sloid:78189"));
		assertEquals("1100008", SwissPtRoutePredictor.simplifyStopId("ch:1:sloid:1100008"));
		assertEquals("StopArea", SwissPtRoutePredictor.simplifyStopId("StopArea:OCE87597674"));
		assertEquals("8014424", SwissPtRoutePredictor.simplifyStopId("8014424"));
	}

	@Test
	public void loopRouteUsesTheRiddenPartOnly() {
		// Route A -> B -> A -> C, boarding at the second A. B is not ridden and has no LOCAL zone.
		TransitSchedule schedule = schedule(List.of("A", "B", "A", "C"), "A", "B", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(LOCAL, SBB), List.of( //
				new Zone(LOCAL, "1", List.of("A", "C")), //
				new Zone(SBB, "1", List.of("A", "B", "C"))));

		// With B, LOCAL would not be common to all the stops; every common network is kept (the pricing chooses)
		SwissPtLegVariables leg = predict(schedule, registry, new NetworkOfDistances(), "A", "C");
		assertEquals(Set.of("LOCAL", "SBB"), leg.zones.keySet());
		assertEquals(List.of("A", "C"), leg.stopIds);
	}

	@Test
	public void sameZoneIdInTwoAuthoritiesKeepsBoth() {
		// Zone "110" exists for both authorities at A; only OTHER is present at both stops.
		TransitSchedule schedule = schedule(List.of("A", "C"), "A", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(LOCAL, OTHER), List.of( //
				new Zone(LOCAL, "110", List.of("A")), //
				new Zone(OTHER, "110", List.of("A", "C"))));

		SwissPtLegVariables leg = predict(schedule, registry, new NetworkOfDistances(), "A", "C");
		assertEquals(Set.of("OTHER"), leg.zones.keySet());
		assertEquals(1, leg.zones.get("OTHER").size());
	}

	@Test
	public void sbbDistanceComesFromTheTableWhenAvailable() {
		TransitSchedule schedule = schedule(List.of("A", "C"), "A", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(SBB), List.of(new Zone(SBB, "1", List.of("A", "C"))));
		NetworkOfDistances distances = new NetworkOfDistances();
		distances.addEdge("A", "C", 7.0);

		assertEquals(7.0, predict(schedule, registry, distances, "A", "C").sbbDistance, 0.0);
	}

	@Test
	public void missingSbbDistanceFallsBackToNetworkDistance() {
		TransitSchedule schedule = schedule(List.of("A", "C"), "A", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(SBB), List.of(new Zone(SBB, "1", List.of("A", "C"))));

		// The leg's routed distance is 5000 m
		assertEquals(5.0, predict(schedule, registry, new NetworkOfDistances(), "A", "C").sbbDistance, 1e-12);
	}

	@Test
	public void nonSbbLegKeepsTheMarker() {
		TransitSchedule schedule = schedule(List.of("A", "C"), "A", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(LOCAL),
				List.of(new Zone(LOCAL, "1", List.of("A", "C"))));

		assertEquals(-1.0, predict(schedule, registry, new NetworkOfDistances(), "A", "C").sbbDistance, 0.0);
	}

	@Test
	public void egressStopNotOnRouteDoesNotFail() {
		// Previously indexOf returned -1 and subList threw
		TransitSchedule schedule = schedule(List.of("A", "B"), "A", "B", "C");
		ZonalRegistry registry = new ZonalRegistry(List.of(LOCAL),
				List.of(new Zone(LOCAL, "1", List.of("A", "C"))));

		assertEquals(Set.of("LOCAL"), predict(schedule, registry, new NetworkOfDistances(), "A", "C").zones.keySet());
	}

	private static SwissPtLegVariables predict(TransitSchedule schedule, ZonalRegistry registry,
			NetworkOfDistances distances, String access, String egress) {
		SwissPtRoutePredictor predictor = new SwissPtRoutePredictor(schedule, registry, distances);

		DefaultTransitPassengerRoute route = new DefaultTransitPassengerRoute(Id.createLinkId("l"),
				Id.createLinkId("l"), Id.create(access, TransitStopFacility.class),
				Id.create(egress, TransitStopFacility.class), Id.create("line", TransitLine.class),
				Id.create("route", TransitRoute.class));
		route.setDistance(5000.0);

		Leg leg = PopulationUtils.createLeg("pt");
		leg.setDepartureTime(8 * 3600.0);
		leg.setTravelTime(600.0);
		leg.setRoute(route);

		List<SwissPtLegVariables> legs = predictor.predictPtVariables(List.of(leg)).legVariables;
		assertEquals(1, legs.size());
		return legs.get(0);
	}

	private static TransitSchedule schedule(List<String> routeStops, String... facilities) {
		TransitSchedule schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		TransitScheduleFactory factory = schedule.getFactory();

		for (String id : facilities) {
			TransitStopFacility facility = factory.createTransitStopFacility(Id.create(id, TransitStopFacility.class),
					new Coord(0, 0), false);
			facility.setName(id);
			schedule.addStopFacility(facility);
		}

		List<TransitRouteStop> stops = new ArrayList<>();
		for (int i = 0; i < routeStops.size(); i++) {
			TransitStopFacility facility = schedule.getFacilities()
					.get(Id.create(routeStops.get(i), TransitStopFacility.class));
			stops.add(factory.createTransitRouteStop(facility, i * 60.0, i * 60.0));
		}

		Id<Link> link = Id.createLinkId("l");
		TransitRoute route = factory.createTransitRoute(Id.create("route", TransitRoute.class),
				RouteUtils.createLinkNetworkRouteImpl(link, link), stops, "bus");
		TransitLine line = factory.createTransitLine(Id.create("line", TransitLine.class));
		line.addRoute(route);
		schedule.addTransitLine(line);

		return schedule;
	}
}
