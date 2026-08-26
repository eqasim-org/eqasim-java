package org.eqasim.core.scenario.cutter.extent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Caches the position of network nodes and links relative to a scenario extent.
 * The attributes are temporary and must be removed before writing the network.
 */
public final class LinkRegionClassifier {
	private static final Logger logger = LogManager.getLogger(LinkRegionClassifier.class);

	public static final String LINK_REGION_ATTRIBUTE = "eqasim:cutter:linkRegion";
	public static final String NODE_INSIDE_ATTRIBUTE = "eqasim:cutter:nodeInside";

	public static final int OUTSIDE = 0;
	public static final int CROSSING = 1;
	public static final int INSIDE = 2;

	private LinkRegionClassifier() {
	}

	public static void classify(Network network, ScenarioExtent extent) {
		logger.info("Classifying network nodes and links ...");
		for (Node node : network.getNodes().values()) {
			node.getAttributes().putAttribute(NODE_INSIDE_ATTRIBUTE, extent.isInside(node.getCoord()));
		}
		int numIn = 0;
		int numOut = 0;
		int numCross = 0;
		for (Link link : network.getLinks().values()) {
			boolean fromInside = isNodeInside(link.getFromNode());
			boolean toInside = isNodeInside(link.getToNode());
			int region = fromInside == toInside ? (fromInside ? INSIDE : OUTSIDE) : CROSSING;
			link.getAttributes().putAttribute(LINK_REGION_ATTRIBUTE, region);
			switch (region) {
				case INSIDE: numIn++;
				case OUTSIDE: numOut++;
				case CROSSING: numCross++;
			}
		}
        logger.info("\t - Number of links inside: {}", numIn);
        logger.info("\t - Number of links outside: {}", numOut);
        logger.info("\t - Number of links crossing: {}", numCross);
	}

	public static int getRegion(Link link) {
		Object value = link.getAttributes().getAttribute(LINK_REGION_ATTRIBUTE);

		if (!(value instanceof Number)) {
			throw new IllegalStateException("Link " + link.getId() + " has not been classified for scenario cutting");
		}

		return ((Number) value).intValue();
	}

	public static boolean isOutside(Link link) {
		return getRegion(link) == OUTSIDE;
	}

	public static boolean isCrossing(Link link) {
		return getRegion(link) == CROSSING;
	}

	public static boolean isInside(Link link) {
		return getRegion(link) == INSIDE;
	}

	public static boolean isFromNodeInside(Link link) {
		return isNodeInside(link.getFromNode());
	}

	private static boolean isNodeInside(Node node) {
		Object value = node.getAttributes().getAttribute(NODE_INSIDE_ATTRIBUTE);

		if (!(value instanceof Boolean)) {
			throw new IllegalStateException("Node " + node.getId() + " has not been classified for scenario cutting");
		}

		return (Boolean) value;
	}

	public static void clear(Network network) {
		for (Link link : network.getLinks().values()) {
			link.getAttributes().removeAttribute(LINK_REGION_ATTRIBUTE);
		}

		for (Node node : network.getNodes().values()) {
			node.getAttributes().removeAttribute(NODE_INSIDE_ATTRIBUTE);
		}
	}
}
