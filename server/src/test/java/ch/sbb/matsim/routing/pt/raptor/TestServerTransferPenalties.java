package ch.sbb.matsim.routing.pt.raptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorUtils;
import org.eqasim.server.services.router.transit.TransitRouterService;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;

/**
 * Checks that the transfer cost calculator used by the server charges each
 * mode-to-mode penalty once per actual transfer (bus -> rail -> bus), with
 * static penalties, request penalties, or both.
 */
public class TestServerTransferPenalties {
	@Test
	public void noPenaltiesUsesDefaultCalculator() {
		assertEquals(0.0, route(Map.of(), null), 1e-12);
	}

	@Test
	public void staticPenaltiesOnly() {
		// Previously DefaultRaptorTransferCostCalculator was used here and rejected the static penalties
		assertEquals(0.1 + 0.3, route(Map.of("bus", Map.of("rail", 0.1), "rail", Map.of("bus", 0.3)), null), 1e-12);
	}

	@Test
	public void requestPenaltiesOnly() {
		assertEquals(0.1 + 0.3, route(Map.of(), Map.of("bus", Map.of("rail", 0.1), "rail", Map.of("bus", 0.3))),
				1e-12);
	}

	@Test
	public void requestPenaltiesOverrideStaticOnes() {
		// bus -> rail from the static configuration, rail -> bus from the request
		assertEquals(0.1 + 0.5, route(Map.of("bus", Map.of("rail", 0.1), "rail", Map.of("bus", 0.3)),
				Map.of("rail", Map.of("bus", 0.5))), 1e-12);
	}

	private static double route(Map<String, Map<String, Double>> staticPenalties,
			Map<String, Map<String, Double>> requestPenalties) {
		Scenario scenario = scenario(staticPenalties);
		Config config = scenario.getConfig();

		EqasimRaptorConfigGroup eqasim = ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class);
		RaptorParameters parameters = EqasimRaptorUtils.createParameters(config, eqasim, scenario.getTransitSchedule(),
				false);
		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);
		assertEquals(!staticPenalties.isEmpty(), staticConfig.isUseModeToModeTransferPenalty());

		SwissRailRaptorData data = SwissRailRaptorData.create(scenario.getTransitSchedule(), null, staticConfig,
				scenario.getNetwork(), null);
		RaptorTransferCostCalculator calculator = TransitRouterService.createTransferCostCalculator(staticConfig,
				requestPenalties);
		SwissRailRaptorCore core = new SwissRailRaptorCore(data, new DefaultRaptorInVehicleCostCalculator(),
				calculator);

		TransitStopFacility from = stop(scenario, "s0");
		TransitStopFacility to = stop(scenario, "s3");
		RaptorRoute result = core.calcLeastCostRoute(90, from, to, List.of(new InitialStop(from, 0, 0, 0, "walk")),
				List.of(new InitialStop(to, 0, 0, 0, "walk")), parameters, null);
		assertNotNull(result);

		List<String> modes = new ArrayList<>();
		for (RaptorRoute.RoutePart part : result.getParts()) {
			if (part.line != null) {
				modes.add(part.route.getTransportMode());
			}
		}
		assertEquals(List.of("bus", "rail", "bus"), modes);
		assertEquals(2, result.getNumberOfTransfers());

		// Range queries go through the same calculator
		List<RaptorRoute> range = core.calcRoutes(90, 90, 110, from, to,
				List.of(new InitialStop(from, 0, 0, 0, "walk")), List.of(new InitialStop(to, 0, 0, 0, "walk")),
				parameters, null);
		assertTrue(!range.isEmpty());
		assertEquals(result.getTotalCosts(), range.get(0).getTotalCosts(), 1e-12);

		return result.getTotalCosts();
	}

	private static TransitStopFacility stop(Scenario scenario, String id) {
		return scenario.getTransitSchedule().getFacilities().get(Id.create(id, TransitStopFacility.class));
	}

	private static Scenario scenario(Map<String, Map<String, Double>> staticPenalties) {
		Config config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup(), new EqasimRaptorConfigGroup());
		config.transit().setUseTransit(true);
		config.transitRouter().setAdditionalTransferTime(0);
		config.transitRouter().setMaxBeelineWalkConnectionDistance(0);
		config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.0);
		config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.0);

		// Only transfer penalties contribute to the cost
		EqasimRaptorConfigGroup eqasim = ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class);
		eqasim.travelTimeBus_u_h = eqasim.travelTimeRail_u_h = eqasim.travelTimeOther_u_h = 0;
		eqasim.travelTimeSubway_u_h = eqasim.travelTimeTram_u_h = 0;
		eqasim.walkTime_u_h = eqasim.waitTime_u_h = eqasim.perTransfer_u = 0;

		SwissRailRaptorConfigGroup raptor = ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class);
		raptor.setTransferWalkMargin(0);
		staticPenalties.forEach((fromMode, penalties) -> penalties.forEach((toMode, penalty) -> raptor
				.addModeToModeTransferPenalty(
						new SwissRailRaptorConfigGroup.ModeToModeTransferPenalty(fromMode, toMode, penalty))));

		Scenario scenario = ScenarioUtils.createScenario(config);
		var network = scenario.getNetwork();
		for (int i = 0; i < 5; i++) {
			NetworkUtils.createAndAddNode(network, Id.createNodeId("n" + i), new Coord(i * 10000, 0));
		}
		for (int i = 0; i < 4; i++) {
			NetworkUtils.createAndAddLink(network, Id.createLinkId("l" + i),
					network.getNodes().get(Id.createNodeId("n" + i)),
					network.getNodes().get(Id.createNodeId("n" + (i + 1))), 10000, 20, 1000, 1);
		}

		var schedule = scenario.getTransitSchedule();
		var factory = schedule.getFactory();
		for (int i = 0; i < 4; i++) {
			TransitStopFacility stop = factory.createTransitStopFacility(Id.create("s" + i, TransitStopFacility.class),
					new Coord(i * 10000, 0), false);
			stop.setLinkId(Id.createLinkId("l" + i));
			schedule.addStopFacility(stop);
		}

		for (int i = 0; i < 3; i++) {
			String mode = i == 1 ? "rail" : "bus";
			String name = "ride" + i;
			TransitStopFacility from = stop(scenario, "s" + i);
			TransitStopFacility to = stop(scenario, "s" + (i + 1));
			var path = RouteUtils.createLinkNetworkRouteImpl(from.getLinkId(), to.getLinkId());
			path.setLinkIds(from.getLinkId(), List.of(), to.getLinkId());
			TransitRoute route = factory.createTransitRoute(Id.create(name, TransitRoute.class), path,
					List.of(factory.createTransitRouteStop(from, 0, 0), factory.createTransitRouteStop(to, 100, 100)),
					mode);
			route.addDeparture(factory.createDeparture(Id.create("departure" + i, Departure.class), 100 + 200 * i));
			TransitLine line = factory.createTransitLine(Id.create(name, TransitLine.class));
			line.addRoute(route);
			schedule.addTransitLine(line);
		}

		return scenario;
	}
}
