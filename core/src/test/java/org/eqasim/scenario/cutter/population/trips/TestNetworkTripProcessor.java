package org.eqasim.scenario.cutter.population.trips;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.NetworkTripProcessor;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPointFinder;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.utils.objectattributes.attributable.Attributes;

public class TestNetworkTripProcessor {
	static private class NetworkFinderMock implements NetworkCrossingPointFinder {
		final private List<NetworkCrossingPoint> points = new LinkedList<>();

		@Override
		public List<NetworkCrossingPoint> findCrossingPoints(String mode, NetworkRoute route, double departureTime) {
			return points;
		}

		public void add(NetworkCrossingPoint point) {
			points.add(point);
		}

		@Override
		public boolean isInside(NetworkRoute route) {
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

		Link linkA = createLinkMock("A");
		Link linkB = createLinkMock("B");

		// No crossing points
		finderMock = new NetworkFinderMock();
		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process("car", null, 100.0, false);

		Assert.assertEquals(1, result.size());
		Assert.assertTrue(result.get(0) instanceof Leg);
		Assert.assertEquals("car", ((Leg) result.get(0)).getMode());

		// One crossing point, outgoing
		finderMock = new NetworkFinderMock();
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, true));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process("car", null, 100.0, false);

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
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, false));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process("car", null, 100.0, false);

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
		finderMock.add(new NetworkCrossingPoint(0, linkA, 10.0, 20.0, true));
		finderMock.add(new NetworkCrossingPoint(0, linkB, 30.0, 40.0, false));

		processor = new NetworkTripProcessor(finderMock, scenarioExtentMock);
		result = processor.process("car", null, 100.0, false);

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
	}

	static private Link createLinkMock(String id) {
		return new Link() {
			@Override
			public Coord getCoord() {
				return null;
			}

			@Override
			public Id<Link> getId() {
				return Id.createLinkId(id);
			}

			@Override
			public Attributes getAttributes() {
				return null;
			}

			@Override
			public boolean setFromNode(Node node) {
				return false;
			}

			@Override
			public boolean setToNode(Node node) {
				return false;
			}

			@Override
			public Node getToNode() {
				return null;
			}

			@Override
			public Node getFromNode() {
				return null;
			}

			@Override
			public double getLength() {
				return 0;
			}

			@Override
			public double getNumberOfLanes() {
				return 0;
			}

			@Override
			public double getNumberOfLanes(double time) {
				return 0;
			}

			@Override
			public double getFreespeed() {
				return 0;
			}

			@Override
			public double getFreespeed(double time) {
				return 0;
			}

			@Override
			public double getCapacity() {
				return 0;
			}

			@Override
			public double getCapacity(double time) {
				return 0;
			}

			@Override
			public void setFreespeed(double freespeed) {
			}

			@Override
			public void setLength(double length) {
			}

			@Override
			public void setNumberOfLanes(double lanes) {
			}

			@Override
			public void setCapacity(double capacity) {
			}

			@Override
			public void setAllowedModes(Set<String> modes) {

			}

			@Override
			public Set<String> getAllowedModes() {
				return null;
			}

			@Override
			public double getFlowCapacityPerSec() {
				return 0;
			}

			@Override
			public double getFlowCapacityPerSec(double time) {
				return 0;
			}
		};
	}
}
