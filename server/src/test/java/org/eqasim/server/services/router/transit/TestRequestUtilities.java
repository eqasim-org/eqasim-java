package org.eqasim.server.services.router.transit;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.server.services.TransitConfiguration;
import org.eqasim.server.services.WalkConfiguration;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;

/**
 * Checks that the utilities of a request reach the routing parameters, in
 * particular other_u_h for the modes which are not rail, subway, bus or tram.
 */
public class TestRequestUtilities {
	@Test
	public void otherUtilityAppliesToNonStandardModes() {
		Scenario scenario = scenario();

		TransitUtilities utilities = new TransitUtilities();
		utilities.rail_u_h = -0.6;
		utilities.bus_u_h = -0.9;
		utilities.other_u_h = -0.8;

		RaptorParameters parameters = TransitRouterService.createParameters(scenario.getConfig(),
				new TransitConfiguration(), new WalkConfiguration(), utilities, scenario.getTransitSchedule());

		assertEquals(-0.6 / 3600.0, parameters.getMarginalUtilityOfTravelTime_utl_s("rail"), 1e-15);
		assertEquals(-0.9 / 3600.0, parameters.getMarginalUtilityOfTravelTime_utl_s("bus"), 1e-15);
		assertEquals(-0.8 / 3600.0, parameters.getMarginalUtilityOfTravelTime_utl_s("ferry"), 1e-15);
		assertEquals(-0.8 / 3600.0, parameters.getMarginalUtilityOfTravelTime_utl_s("funicular"), 1e-15);
	}

	@Test
	public void withoutOtherUtilityTheConfigurationIsUsed() {
		Scenario scenario = scenario();

		TransitUtilities utilities = new TransitUtilities();
		utilities.rail_u_h = -0.6;

		RaptorParameters parameters = TransitRouterService.createParameters(scenario.getConfig(),
				new TransitConfiguration(), new WalkConfiguration(), utilities, scenario.getTransitSchedule());

		assertEquals(-2.5 / 3600.0, parameters.getMarginalUtilityOfTravelTime_utl_s("ferry"), 1e-15);
	}

	private static Scenario scenario() {
		Config config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup(), new EqasimRaptorConfigGroup());
		EqasimRaptorConfigGroup eqasim = ConfigUtils.addOrGetModule(config, EqasimRaptorConfigGroup.class);
		eqasim.travelTimeOther_u_h = -2.5;

		Scenario scenario = ScenarioUtils.createScenario(config);
		var schedule = scenario.getTransitSchedule();
		var factory = schedule.getFactory();

		TransitStopFacility from = factory.createTransitStopFacility(Id.create("a", TransitStopFacility.class),
				new Coord(0, 0), false);
		TransitStopFacility to = factory.createTransitStopFacility(Id.create("b", TransitStopFacility.class),
				new Coord(1000, 0), false);
		from.setLinkId(Id.createLinkId("l"));
		to.setLinkId(Id.createLinkId("l"));
		schedule.addStopFacility(from);
		schedule.addStopFacility(to);

		for (String mode : List.of("rail", "bus", "ferry", "funicular")) {
			var path = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("l"), Id.createLinkId("l"));
			TransitRoute route = factory.createTransitRoute(Id.create(mode, TransitRoute.class), path,
					List.of(factory.createTransitRouteStop(from, 0, 0), factory.createTransitRouteStop(to, 60, 60)),
					mode);
			TransitLine line = factory.createTransitLine(Id.create(mode, TransitLine.class));
			line.addRoute(route);
			schedule.addTransitLine(line);
		}

		return scenario;
	}
}
