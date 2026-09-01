package org.eqasim.scenario.cutter.extent;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eqasim.core.scenario.cutter.extent.LinkRegionClassifier;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

public class TestLinkRegionClassifier {
	@Test
	public void classifiesEachNodeOnceAndClearsTemporaryAttributes() {
		Network network = NetworkUtils.createNetwork();
		Node outsideA = createNode(network, "outsideA", 0.0);
		Node outsideB = createNode(network, "outsideB", 1.0);
		Node insideA = createNode(network, "insideA", 2.0);
		Node insideB = createNode(network, "insideB", 3.0);

		Link outside = createLink(network, "outside", outsideA, outsideB);
		Link incoming = createLink(network, "incoming", outsideB, insideA);
		Link inside = createLink(network, "inside", insideA, insideB);
		Link outgoing = createLink(network, "outgoing", insideB, outsideA);

		AtomicInteger geometryChecks = new AtomicInteger();
		ScenarioExtent extent = new ScenarioExtent() {
			@Override
			public boolean isInside(Coord coord) {
				geometryChecks.incrementAndGet();
				return coord.getX() >= 2.0;
			}

			@Override
			public List<Coord> computeEuclideanIntersections(Coord from, Coord to) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Coord getInteriorPoint() {
				throw new UnsupportedOperationException();
			}
		};

		LinkRegionClassifier.classify(network, extent);

		Assert.assertEquals(network.getNodes().size(), geometryChecks.get());
		Assert.assertEquals(LinkRegionClassifier.OUTSIDE, LinkRegionClassifier.getRegion(outside));
		Assert.assertEquals(LinkRegionClassifier.CROSSING, LinkRegionClassifier.getRegion(incoming));
		Assert.assertEquals(LinkRegionClassifier.INSIDE, LinkRegionClassifier.getRegion(inside));
		Assert.assertEquals(LinkRegionClassifier.CROSSING, LinkRegionClassifier.getRegion(outgoing));
		Assert.assertFalse(LinkRegionClassifier.isFromNodeInside(incoming));
		Assert.assertTrue(LinkRegionClassifier.isFromNodeInside(outgoing));

		LinkRegionClassifier.clear(network);
		for (Link link : network.getLinks().values()) {
			Assert.assertNull(link.getAttributes().getAttribute(LinkRegionClassifier.LINK_REGION_ATTRIBUTE));
		}
		for (Node node : network.getNodes().values()) {
			Assert.assertNull(node.getAttributes().getAttribute(LinkRegionClassifier.NODE_INSIDE_ATTRIBUTE));
		}
	}

	private Node createNode(Network network, String id, double x) {
		Node node = network.getFactory().createNode(Id.createNodeId(id), new Coord(x, 0.0));
		network.addNode(node);
		return node;
	}

	private Link createLink(Network network, String id, Node fromNode, Node toNode) {
		Link link = network.getFactory().createLink(Id.createLinkId(id), fromNode, toNode);
		network.addLink(link);
		return link;
	}
}
