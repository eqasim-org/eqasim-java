package org.eqasim.server;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eqasim.server.api.InterfaceMetadata;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ProjectionUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.transitSchedule.api.Departure;

import com.fasterxml.jackson.databind.ObjectMapper;

public class TestInterfaceMetadata {
	private Network network() {
		Network network = NetworkUtils.createNetwork();
		network.addNode(network.getFactory().createNode(Id.createNodeId("a"), new Coord(600000, 6800000)));
		network.addNode(network.getFactory().createNode(Id.createNodeId("b"), new Coord(700000, 6900000)));
		return network;
	}

	@Test
	public void cleanedExtentAndProjectionDefinitions() throws Exception {
		Network original = network(), cleaned = network();
		original.addNode(original.getFactory().createNode(Id.createNodeId("excluded"), new Coord(-100, -200)));
		List<Object> fixtures = new ArrayList<>();
		for (var entry : Map.of("EPSG:2154", new double[] { 2.65, 48.53 },
				"EPSG:2056", new double[] { 8.54, 47.37 }, "EPSG:2227", new double[] { -122.42, 37.77 },
				"EPSG:29183", new double[] { -46.63, -23.55 }).entrySet()) {
			ProjectionUtils.putCRS(original, entry.getKey());
			var metadata = InterfaceMetadata.create(original, cleaned, null);
			assertEquals(entry.getKey(), metadata.crs());
			assertEquals(600000, metadata.networkExtent().minX(), 0);
			assertEquals(6900000, metadata.networkExtent().maxY(), 0);
			assertEquals(1, metadata.scheduleDayCount());
			assertTrue(metadata.projectionDefinition().startsWith("PROJCS["));
			var transform = CRS.findMathTransform(CRS.decode("EPSG:4326", true), CRS.decode(entry.getKey(), true), true);
			double[] xy = new double[2];
			transform.transform(entry.getValue(), 0, xy, 0, 1);
			fixtures.add(Map.of("metadata", metadata, "geographic", entry.getValue(), "projected", xy));
		}
		// Reusable by browser verification to compare proj4js against GeoTools across CRSs.
		Files.writeString(Path.of("target/interface-projections.json"), new ObjectMapper().writeValueAsString(fixtures));
	}

	@Test
	public void invalidMetadataFailsClearly() {
		Network original = network();
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> InterfaceMetadata.create(original, network(), null)).getMessage().contains("no CRS"));
		ProjectionUtils.putCRS(original, "EPSG:9999999");
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> InterfaceMetadata.create(original, network(), null)).getMessage().contains("browser projection"));
		ProjectionUtils.putCRS(original, "EPSG:2154");
		assertTrue(assertThrows(IllegalArgumentException.class,
				() -> InterfaceMetadata.create(original, NetworkUtils.createNetwork(), null)).getMessage().contains("degenerate extent"));
		Network degenerate = NetworkUtils.createNetwork();
		degenerate.addNode(degenerate.getFactory().createNode(Id.createNodeId("a"), new Coord(1, 1)));
		assertThrows(IllegalArgumentException.class, () -> InterfaceMetadata.create(original, degenerate, null));
	}

	private TransitSchedule schedule(double departureTime, double arrivalOffset, double departureOffset) {
		var schedule = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getTransitSchedule();
		var factory = schedule.getFactory();
		var facility = factory.createTransitStopFacility(Id.create("stop", TransitStopFacility.class), new Coord(0, 0), false);
		schedule.addStopFacility(facility);
		var stop = factory.createTransitRouteStop(facility,
				Double.isNaN(arrivalOffset) ? OptionalTime.undefined() : OptionalTime.defined(arrivalOffset),
				Double.isNaN(departureOffset) ? OptionalTime.undefined() : OptionalTime.defined(departureOffset));
		var route = factory.createTransitRoute(Id.create("route", TransitRoute.class), null, List.of(stop), "bus");
		route.addDeparture(factory.createDeparture(Id.create("departure", Departure.class), departureTime));
		var line = factory.createTransitLine(Id.create("line", TransitLine.class));
		line.addRoute(route); schedule.addTransitLine(line);
		return schedule;
	}

	@Test
	public void continuousScheduleDays() {
		assertEquals(1, InterfaceMetadata.scheduleDayCount(null));
		assertEquals(1, InterfaceMetadata.scheduleDayCount(schedule(28800, 100, 120)));
		assertEquals(1, InterfaceMetadata.scheduleDayCount(schedule(86300, 99, Double.NaN)));
		assertEquals(2, InterfaceMetadata.scheduleDayCount(schedule(86300, 100, Double.NaN)));
		assertEquals(2, InterfaceMetadata.scheduleDayCount(schedule(86300, Double.NaN, 101)));
		assertEquals(10, InterfaceMetadata.scheduleDayCount(schedule(8 * 86400, 86400, 1)));
		var schedule = schedule(28800, 60, 120);
		var route = schedule.getTransitLines().values().iterator().next().getRoutes().values().iterator().next();
		route.addDeparture(schedule.getFactory().createDeparture(Id.create("late", Departure.class), 7 * 86400));
		assertEquals(8, InterfaceMetadata.scheduleDayCount(schedule));
		route.addDeparture(schedule.getFactory().createDeparture(Id.create("infinite", Departure.class), Double.POSITIVE_INFINITY));
		assertEquals(8, InterfaceMetadata.scheduleDayCount(schedule));
	}
}
