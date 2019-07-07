package org.eqasim.scenario.cutter.population.trips.crossing;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.DefaultNetworkCrossingPointFinder;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPoint;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.NetworkCrossingPointFinder;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

public class TestDefaultNetworkCrossingPointFinder {
	static final private TravelTime travelTimeMock = new TravelTime() {
		@Override
		public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
			return 10.0;
		}
	};

	static final private Network networkMock;

	static {
		networkMock = NetworkUtils.createNetwork();

		Node node1 = networkMock.getFactory().createNode(Id.createNodeId("1"), new Coord(1.0, 0.0));
		Node node2 = networkMock.getFactory().createNode(Id.createNodeId("2"), new Coord(2.0, 0.0));
		Node node3 = networkMock.getFactory().createNode(Id.createNodeId("3"), new Coord(3.0, 0.0));
		Node node4 = networkMock.getFactory().createNode(Id.createNodeId("4"), new Coord(4.0, 0.0));
		Node node5 = networkMock.getFactory().createNode(Id.createNodeId("5"), new Coord(5.0, 0.0));
		Node node6 = networkMock.getFactory().createNode(Id.createNodeId("6"), new Coord(6.0, 0.0));
		Node node7 = networkMock.getFactory().createNode(Id.createNodeId("7"), new Coord(7.0, 0.0));
		Node node8 = networkMock.getFactory().createNode(Id.createNodeId("8"), new Coord(8.0, 0.0));
		Node node9 = networkMock.getFactory().createNode(Id.createNodeId("9"), new Coord(9.0, 0.0));

		Link link12 = networkMock.getFactory().createLink(Id.createLinkId("12"), node1, node2);
		Link link23 = networkMock.getFactory().createLink(Id.createLinkId("23"), node2, node3);
		Link link34 = networkMock.getFactory().createLink(Id.createLinkId("34"), node3, node4);
		Link link45 = networkMock.getFactory().createLink(Id.createLinkId("45"), node4, node5);
		Link link56 = networkMock.getFactory().createLink(Id.createLinkId("56"), node5, node6);
		Link link67 = networkMock.getFactory().createLink(Id.createLinkId("67"), node6, node7);
		Link link78 = networkMock.getFactory().createLink(Id.createLinkId("78"), node7, node8);
		Link link89 = networkMock.getFactory().createLink(Id.createLinkId("89"), node8, node9);

		Arrays.asList(node1, node2, node3, node4, node5, node6, node7, node8, node9).forEach(networkMock::addNode);
		Arrays.asList(link12, link23, link34, link45, link56, link67, link78, link89).forEach(networkMock::addLink);
	}

	final private static ScenarioExtent extentMock = new ScenarioExtent() {
		@Override
		public boolean isInside(Coord coord) {
			return coord.getX() == 3.0 || coord.getX() == 4.0 || coord.getX() == 6.0 || coord.getX() == 7.0;
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

	final static NetworkRoute createRouteMock(int startId, int endId) {
		Id<Link> startLinkId = Id.createLinkId(String.format("%d%d", startId, startId + 1));
		Id<Link> endLinkId = Id.createLinkId(String.format("%d%d", endId - 1, endId));
		List<Id<Link>> linkIds = new LinkedList<>();

		for (int i = startId + 1; i < endId - 1; i++) {
			linkIds.add(Id.createLinkId(String.format("%d%d", i, i + 1)));
		}

		NetworkRoute route = (NetworkRoute) new LinkNetworkRouteFactory().createRoute(startLinkId, endLinkId);
		route.setLinkIds(startLinkId, linkIds, endLinkId);

		return route;
	}

	@Test
	public void testFindCrossingPoints() {
		NetworkCrossingPointFinder finder = new DefaultNetworkCrossingPointFinder(extentMock, networkMock,
				Collections.singletonMap("car", travelTimeMock));

		NetworkRoute route;
		List<NetworkCrossingPoint> result;

		// 1) Outside -> Inside
		route = createRouteMock(1, 3);
		result = finder.findCrossingPoints("car", route, 100.0);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(110.0, result.get(0).enterTime, 1e-3);
		Assert.assertEquals(120.0, result.get(0).leaveTime, 1e-3);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("23")), result.get(0).link);
		Assert.assertFalse(result.get(0).isOutgoing);

		route = createRouteMock(1, 4);
		result = finder.findCrossingPoints("car", route, 100.0);

		route = createRouteMock(2, 4);
		result = finder.findCrossingPoints("car", route, 100.0);

		// 2) Inside -> Outside
		route = createRouteMock(6, 9);
		result = finder.findCrossingPoints("car", route, 100.0);
		Assert.assertEquals(1, result.size());
		Assert.assertEquals(110.0, result.get(0).enterTime, 1e-3);
		Assert.assertEquals(120.0, result.get(0).leaveTime, 1e-3);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("78")), result.get(0).link);
		Assert.assertTrue(result.get(0).isOutgoing);

		route = createRouteMock(7, 9);
		Assert.assertEquals(110.0, result.get(0).enterTime, 1e-3);

		route = createRouteMock(6, 8);
		Assert.assertEquals(110.0, result.get(0).enterTime, 1e-3);

		// 3) Inside -> Outside -> Inside
		route = createRouteMock(4, 7);
		result = finder.findCrossingPoints("car", route, 100.0);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(100.0, result.get(0).enterTime, 1e-3);
		Assert.assertEquals(110.0, result.get(0).leaveTime, 1e-3);
		Assert.assertEquals(110.0, result.get(1).enterTime, 1e-3);
		Assert.assertEquals(120.0, result.get(1).leaveTime, 1e-3);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("45")), result.get(0).link);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("56")), result.get(1).link);
		Assert.assertTrue(result.get(0).isOutgoing);
		Assert.assertFalse(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside
		route = createRouteMock(1, 5);
		result = finder.findCrossingPoints("car", route, 100.0);
		Assert.assertEquals(2, result.size());
		Assert.assertEquals(110.0, result.get(0).enterTime, 1e-3);
		Assert.assertEquals(120.0, result.get(0).leaveTime, 1e-3);
		Assert.assertEquals(130.0, result.get(1).enterTime, 1e-3);
		Assert.assertEquals(140.0, result.get(1).leaveTime, 1e-3);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("23")), result.get(0).link);
		Assert.assertEquals(networkMock.getLinks().get(Id.createLinkId("45")), result.get(1).link);
		Assert.assertFalse(result.get(0).isOutgoing);
		Assert.assertTrue(result.get(1).isOutgoing);

		// 4) Outside -> Inside -> Outside -> Inside -> Outside
		route = createRouteMock(1, 9);
		result = finder.findCrossingPoints("car", route, 100.0);
		Assert.assertEquals(4, result.size());
	}
}
