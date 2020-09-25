package org.eqasim.scenario.cutter.population.trips;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.TransitTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitRouteCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.transit.TransitTripCrossingPointFinder;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestTransitTripProcessor {
	static private class PublicTransitFinderMock implements TransitTripCrossingPointFinder {
		final private List<TransitTripCrossingPoint> points = new LinkedList<>();

		public void add(TransitTripCrossingPoint point) {
			points.add(point);
		}

		@Override
		public List<TransitTripCrossingPoint> findCrossingPoints(Coord startCoord, List<PlanElement> trip,
				Coord endCoord) {
			return points;
		}
	}

	static ScenarioExtent scenarioExtentMock = new ScenarioExtent() {
		@Override
		public boolean isInside(Coord coord) {
			return false;
		}

		@Override
		public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
			return null;
		}

		@Override
		public Coord getInteriorPoint() {
			return null;
		}
	};

	@Test
	public void testPublicTransitTripProcessor() {
		PublicTransitFinderMock finderMock;
		TransitTripProcessor processor;
		List<PlanElement> result;

		TransitScheduleFactory factory = new TransitScheduleFactoryImpl();
		TransitStopFacility outsideStopFacility = factory
				.createTransitStopFacility(Id.create("O", TransitStopFacility.class), new Coord(1.0, 0.0), false);
		TransitRouteStop outsideStop = factory.createTransitRouteStop(outsideStopFacility, 0.0, 0.0);
		TransitStopFacility insideStopFacility = factory
				.createTransitStopFacility(Id.create("I", TransitStopFacility.class), new Coord(2.0, 0.0), false);
		TransitRouteStop insideStop = factory.createTransitRouteStop(insideStopFacility, 0.0, 0.0);

		// No crossing points
		finderMock = new PublicTransitFinderMock();
		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertEquals("pt", ((Leg) result.get(0)).getMode());

		// One crossing point, outgoing, in vehicle
		finderMock = new PublicTransitFinderMock();
		finderMock.add(new TransitTripCrossingPoint(
				new TransitRouteCrossingPoint(null, null, outsideStop, insideStop, 20.0, 10.0, true)));

		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("pt", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(9.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(new Coord(2.0, 0.0), ((Activity) result.get(1)).getCoord());

		// One crossing point, outgoing, transfer
		finderMock = new PublicTransitFinderMock();
		finderMock.add(new TransitTripCrossingPoint(new TeleportationCrossingPoint(new Coord(5.0, 0.0), 50.0, true)));

		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("pt", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(50.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(new Coord(5.0, 0.0), ((Activity) result.get(1)).getCoord());

		// One crossing point, incoming, in vehicle
		finderMock = new PublicTransitFinderMock();
		finderMock.add(new TransitTripCrossingPoint(
				new TransitRouteCrossingPoint(null, null, outsideStop, insideStop, 20.0, 10.0, false)));

		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("outside", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("pt", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(9.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(new Coord(2.0, 0.0), ((Activity) result.get(1)).getCoord());

		// One crossing point, incoming, transfer
		finderMock = new PublicTransitFinderMock();
		finderMock.add(new TransitTripCrossingPoint(new TeleportationCrossingPoint(new Coord(5.0, 0.0), 50.0, false)));

		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("outside", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("pt", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(50.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(new Coord(5.0, 0.0), ((Activity) result.get(1)).getCoord());

		// Two crossing points, inside -> outside -> inside
		finderMock = new PublicTransitFinderMock();
		finderMock.add(new TransitTripCrossingPoint(
				new TransitRouteCrossingPoint(null, null, outsideStop, insideStop, 20.0, 10.0, true)));
		finderMock.add(new TransitTripCrossingPoint(new TeleportationCrossingPoint(new Coord(5.0, 0.0), 50.0, false)));

		processor = new TransitTripProcessor(finderMock, scenarioExtentMock, 1.0);
		result = processor.process(new Coord(0.0, 0.0), Collections.emptyList(), new Coord(0.0, 0.0), false);

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("pt", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("pt", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(9.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(50.0, ((Activity) result.get(3)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(new Coord(2.0, 0.0), ((Activity) result.get(1)).getCoord());
		Assert.assertEquals(new Coord(5.0, 0.0), ((Activity) result.get(3)).getCoord());
	}
}
