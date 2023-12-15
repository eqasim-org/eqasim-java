package org.eqasim.scenario.cutter.population.trips;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.NetworkTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkRouteCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkTripCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkTripCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation.TeleportationCrossingPoint;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.mockito.Mockito;

public class TestNetworkTripProcessor {
	static private class NetworkFinderMock implements NetworkTripCrossingPointFinder {
		final private List<NetworkTripCrossingPoint> points = new LinkedList<>();

		@Override
		public List<NetworkTripCrossingPoint> findCrossingPoints(Id<Person> personId, int firstLegIndex,
				Coord startCoord, List<PlanElement> trip, Coord endCoord) {
			return points;
		}

		public void add(NetworkTripCrossingPoint point) {
			points.add(point);
		}

		@Override
		public boolean isInside(List<PlanElement> trip) {
			return true;
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
	public void testCarTripProcessor() {
		NetworkFinderMock finderMock;
		NetworkTripProcessor processor;
		List<PlanElement> result;

		Node mockNode = Mockito.mock(Node.class);
		Mockito.when(mockNode.getCoord()).thenReturn(new Coord(0.0, 0.0));

		Link linkA = Mockito.mock(Link.class);
		Mockito.when(linkA.getId()).thenReturn(Id.createLinkId("A"));
		Mockito.when(linkA.getToNode()).thenReturn(mockNode);
		
		Link linkB = Mockito.mock(Link.class);
		Mockito.when(linkB.getId()).thenReturn(Id.createLinkId("B"));
		Mockito.when(linkB.getToNode()).thenReturn(mockNode);

		Leg mockLeg = Mockito.mock(Leg.class);
		Mockito.when(mockLeg.getDepartureTime()).thenReturn(OptionalTime.defined(100.0));
		Mockito.when(mockLeg.getMode()).thenReturn("car");

		Activity activity = Mockito.mock(Activity.class);
		Mockito.when(activity.getCoord()).thenReturn(new Coord(0.0, 0.0));

		// No crossing points
		finderMock = new NetworkFinderMock();
		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockLeg), activity, "car");

		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());

		// One crossing point, outgoing
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkA, 10.0, 20.0, true), "car"));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockLeg), activity, "car");

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());

		// One crossing point, incoming
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkA, 10.0, 20.0, false), "car"));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockLeg), activity, "car");

		Assert.assertEquals(3, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertEquals("outside", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("car", ((Leg) result.get(2)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());

		// Two crossing points, inside -> outside -> inside
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkA, 10.0, 20.0, true), "car"));
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkB, 30.0, 40.0, false), "car"));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockLeg), activity, "car");

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("car", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(20.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(40.0, ((Activity) result.get(3)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(Id.createLinkId("A"), ((Activity) result.get(1)).getLinkId());
		Assert.assertEquals(Id.createLinkId("B"), ((Activity) result.get(3)).getLinkId());

		// Crossing point at the access leg
		Leg mockAccessLeg = Mockito.mock(Leg.class);
		Mockito.when(mockAccessLeg.getMode()).thenReturn("walk");

		Activity mockInteractionActivity = Mockito.mock(Activity.class);
		Mockito.when(mockInteractionActivity.getType()).thenReturn("car interaction");

		finderMock = new NetworkFinderMock();
		finderMock.add(
				new NetworkTripCrossingPoint(new TeleportationCrossingPoint(new Coord(0.0, 0.0), 10.0, true), "walk"));
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkB, 30.0, 40.0, false), "car"));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockAccessLeg, mockInteractionActivity, mockLeg),
				activity, "car");

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("walk", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("car", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(10.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(40.0, ((Activity) result.get(3)).getEndTime().seconds(), 1e-3);

		// Crossing point at the egress leg
		Leg mockEgressLeg = Mockito.mock(Leg.class);
		Mockito.when(mockEgressLeg.getMode()).thenReturn("walk");

		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkTripCrossingPoint(new NetworkRouteCrossingPoint(0, linkB, 30.0, 40.0, true), "car"));
		finderMock.add(
				new NetworkTripCrossingPoint(new TeleportationCrossingPoint(new Coord(0.0, 0.0), 70.0, false), "walk"));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process(null, 0, activity, Arrays.asList(mockLeg, mockInteractionActivity, mockEgressLeg),
				activity, "car");

		Assert.assertEquals(5, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertTrue(result.get(1) instanceof Activity);
		Assert.assertTrue(result.get(2) instanceof Leg);
		Assert.assertTrue(result.get(3) instanceof Activity);
		Assert.assertTrue(result.get(4) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(1)).getType());
		Assert.assertEquals("outside", ((Leg) result.get(2)).getMode());
		Assert.assertEquals("outside", ((Activity) result.get(3)).getType());
		Assert.assertEquals("walk", ((Leg) result.get(4)).getMode());
		Assert.assertEquals(40.0, ((Activity) result.get(1)).getEndTime().seconds(), 1e-3);
		Assert.assertEquals(70.0, ((Activity) result.get(3)).getEndTime().seconds(), 1e-3);
	}
}
