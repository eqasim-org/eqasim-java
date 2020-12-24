package org.eqasim.scenario.cutter.population.trips.crossing;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.transit.departure.DefaultDepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder.NoDepartureFoundException;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.DefaultTransitRouteCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitRouteCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitRouteCrossingPointFinder;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestDefaultTransitRouteCrossingPointFinder {
	final private static TransitSchedule transitSchedule;
	final private static TransitLine transitLine;
	final private static TransitRoute transitRoute;
	final private static Departure departure;

	static {
		/*
		 * Line departs @ t = [10, 20, 30, 40, 50, 60, 70], has stops @ x = [1.0 ...
		 * 10.0], needs 5s per stop
		 */

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		List<TransitRouteStop> stops = new LinkedList<>();
		List<TransitStopFacility> facilities = new LinkedList<>();

		for (int i = 1; i <= 10; i++) {
			TransitStopFacility facility = factory.createTransitStopFacility(
					Id.create("fac" + i, TransitStopFacility.class), new Coord(i, 0.0), false);
			stops.add(factory.createTransitRouteStop(facility, (i - 1) * 5.0 - 1.0, (i - 1) * 5.0));
			facilities.add(facility);
		}

		transitRoute = factory.createTransitRoute(Id.create("R", TransitRoute.class), null, stops, "rail");
		transitLine = factory.createTransitLine(Id.create("R", TransitLine.class));
		transitSchedule = factory.createTransitSchedule();
		facilities.forEach(transitSchedule::addStopFacility);
		transitSchedule.addTransitLine(transitLine);
		transitLine.addRoute(transitRoute);

		departure = factory.createDeparture(Id.create("dep", Departure.class), 50.0);
		transitRoute.addDeparture(departure);
	}

	final private static TransitPassengerRoute routeMock = new DefaultTransitPassengerRoute(null, null,
			transitRoute.getStops().get(3).getStopFacility().getId(),
			transitRoute.getStops().get(6).getStopFacility().getId(), transitLine.getId(), transitRoute.getId());

	static ScenarioExtent createExtentMock(double... inside) {
		List<Double> _inside = new LinkedList<>();

		for (int i = 0; i < inside.length; i++) {
			_inside.add(inside[i]);
		}

		return new ScenarioExtent() {
			@Override
			public boolean isInside(Coord coord) {
				return _inside.contains(coord.getX());
			}

			@Override
			public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
				throw new IllegalStateException();
			}

			@Override
			public Coord getInteriorPoint() {
				return null;
			}
		};
	}

	@Test
	public void testFindCrossingPoints() throws NoDepartureFoundException {
		ScenarioExtent extent;
		TransitRouteCrossingPointFinder finder;
		List<TransitRouteCrossingPoint> result;

		DepartureFinder departureFinder = new DefaultDepartureFinder();

		// 1) Outside -> Inside
		extent = createExtentMock(6.0, 7.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(0).insideStop);
		Assert.assertFalse(result.get(0).isOutgoing);

		// 2) Inside -> Outside
		extent = createExtentMock(4.0, 5.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(0).outsideStop);
		Assert.assertTrue(result.get(0).isOutgoing);

		// 3) Inside -> Outside -> Inside
		extent = createExtentMock(4.0, 7.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(2, result.size());
		Assert.assertEquals(50.0 + 15.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(3), result.get(0).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).outsideStop);
		Assert.assertTrue(result.get(0).isOutgoing);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(1).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 15.0, result.get(1).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(1).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(6), result.get(1).insideStop);
		Assert.assertFalse(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside
		extent = createExtentMock(5.0, 6.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
		result = finder.findCrossingPoints(routeMock, 25.0);

		Assert.assertEquals(2, result.size());
		Assert.assertEquals(50.0 + 15.0, result.get(0).outsideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 5.0, result.get(0).insideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(3), result.get(0).outsideStop);
		Assert.assertEquals(transitRoute.getStops().get(4), result.get(0).insideStop);
		Assert.assertFalse(result.get(0).isOutgoing);
		Assert.assertEquals(50.0 + 15.0 + 10.0, result.get(1).insideDepartureTime, 1e-3);
		Assert.assertEquals(50.0 + 15.0 + 15.0, result.get(1).outsideDepartureTime, 1e-3);
		Assert.assertEquals(transitRoute.getStops().get(5), result.get(1).insideStop);
		Assert.assertEquals(transitRoute.getStops().get(6), result.get(1).outsideStop);
		Assert.assertTrue(result.get(1).isOutgoing);

		// 5) Inside -> Outside -> Inside -> Outside
		extent = createExtentMock(4.0, 6.0);
		finder = new DefaultTransitRouteCrossingPointFinder(extent, transitSchedule, departureFinder);
		result = finder.findCrossingPoints(routeMock, 25.0);
		Assert.assertEquals(3, result.size());
	}
}
