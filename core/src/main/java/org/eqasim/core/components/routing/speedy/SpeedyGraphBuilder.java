/* Adapted from MATSim 2026.0-2025w33, https://github.com/matsim-org/matsim-libs.
 * Original authors: Marcel Rieser / Simunto (and contributors named below).
 * Modified for Eqasim on 2026-10-15: compact storage and bounded preprocessing.
 * Distributed under the GNU GPL v2 or later, without warranty; see /LICENSE.
 */
package org.eqasim.core.components.routing.speedy;

import jakarta.annotation.Nullable;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.turnRestrictions.TurnRestrictionsContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Creates a {@link SpeedyGraph} for a provided {@link Network}.
 *
 * @author mrieser / Simunto
 */
public class SpeedyGraphBuilder {

	private int nodeCount;
	private int linkCount;
	private int[] nodeData;
	private int[] linkData;
	private Link[] links;
	private Node[] nodes;

	@Deprecated // use build-method with additional mode argument
	public static SpeedyGraph build(Network network) {
		return build(network, null);
	}

	/**
	 * Builds a routing graph from a network considering all links without caring
	 * about allowedModes but considering {@link DisallowedNextLinks} (aka turn
	 * restrictions) of the given {@code turnRestrictionsMode}.
	 *
	 * If turnRestrictionsMode == null, see
	 * {@link TurnRestrictionsContext#build(Network, String)} for how this is
	 * handled.
	 *
	 * @param network
	 * @param turnRestrictionsMode
	 * @return
	 */
	public static SpeedyGraph build(Network network, @Nullable String turnRestrictionsMode) {
		if (NetworkUtils.hasTurnRestrictions(network)) {
			return new SpeedyGraphBuilder().buildWithTurnRestrictions(network, turnRestrictionsMode);
		}
		return new SpeedyGraphBuilder().buildWithoutTurnRestrictions(network);
	}

	private int[] nodeIndices;
    private int[] linkIndices;

    private SpeedyGraph buildWithTurnRestrictions(Network network, @Nullable String mode) {
        // Reuse MATSim's complete multi-link restriction expansion unchanged.
        return buildCompact(network, TurnRestrictionsContext.build(network, mode));
    }

    private SpeedyGraph buildWithoutTurnRestrictions(Network network) {
        return buildCompact(network, null);
    }

    private SpeedyGraph buildCompact(Network network, TurnRestrictionsContext context) {
        int legacyNodeSlots = context == null ? Id.getNumberOfIds(Node.class) : context.getNodeCount();
        int legacyLinkSlots = context == null ? Id.getNumberOfIds(Link.class) : context.getLinkCount();
        var originals = new ArrayList<>(network.getNodes().values());
        originals.sort(java.util.Comparator.comparingInt(n -> n.getId().index()));
        var colored = context == null ? new ArrayList<TurnRestrictionsContext.ColoredNode>()
                : new ArrayList<>(context.coloredNodes);
        colored.sort(java.util.Comparator.comparingInt(TurnRestrictionsContext.ColoredNode::index));
        this.nodeCount = originals.size() + colored.size();
        this.nodeIndices = new int[legacyNodeSlots];
        Arrays.fill(nodeIndices, -1);
        this.nodes = new Node[nodeCount];
        int next = 0;
        for (Node node : originals) {
            nodeIndices[node.getId().index()] = next;
            nodes[next++] = node;
        }
        for (var node : colored) {
            nodeIndices[node.index()] = next;
            nodes[next++] = node.node();
        }

        // Assign monotonically increasing internal IDs. Relative order, adjacency
        // traversal and priority-queue tie breaking remain identical to MATSim.
        this.linkIndices = new int[legacyLinkSlots];
        Arrays.fill(linkIndices, -1);
        for (Link link : network.getLinks().values()) {
            if (context == null || !context.replacedLinks.containsKey(link.getId())) {
                linkIndices[link.getId().index()] = 0;
            }
        }
        if (context != null) for (var link : context.coloredLinks) linkIndices[link.index] = 0;
        this.linkCount = 0;
        for (int i = 0; i < linkIndices.length; i++) {
            if (linkIndices[i] == 0) linkIndices[i] = linkCount++;
        }
        this.nodeData = new int[nodeCount * SpeedyGraph.NODE_SIZE];
        this.linkData = new int[linkCount * SpeedyGraph.LINK_SIZE];
        this.links = new Link[linkCount];
        Arrays.fill(nodeData, -1);
        Arrays.fill(linkData, -1);
        List<Id<Link>> ids = new ArrayList<>(network.getLinks().keySet());
        Collections.sort(ids);
        for (Id<Link> id : ids) {
            if (context == null || !context.replacedLinks.containsKey(id)) addLink(network.getLinks().get(id));
        }
        if (context != null) for (var link : context.coloredLinks) addLink(link);

        int restrictedCount = context == null ? 0 : context.replacedLinks.size();
        int[] restrictedIds = new int[restrictedCount];
        int[] restrictedStarts = new int[restrictedCount];
        if (context != null) {
            int[] sortedIds = context.replacedLinks.keySet().stream().mapToInt(Id::index).sorted().toArray();
            var starts = new java.util.HashMap<Integer, Integer>();
            context.replacedLinks.forEach((id, link) -> starts.put(id.index(), nodeIndices[link.toColoredNode.index()]));
            for (int i = 0; i < sortedIds.length; i++) {
                restrictedIds[i] = sortedIds[i];
                restrictedStarts[i] = starts.get(sortedIds[i]);
            }
        }
        // Only compact endpoint IDs and colored-to-original indices survive.
        // No global IDs are created or reset by this implementation.
        int[] realIds = originals.stream().mapToInt(n -> n.getId().index()).toArray();
        int[] coloredOriginals = colored.stream().mapToInt(n -> nodeIndices[n.node().getId().index()]).toArray();
        return new SpeedyGraph(nodeData, linkData, nodes, links, originals.size(), legacyNodeSlots,
                realIds, coloredOriginals, restrictedIds, restrictedStarts, context != null);
    }

	private void addLink(Link link) {
		int fromNodeIdx = nodeIndices[link.getFromNode().getId().index()];
		int toNodeIdx = nodeIndices[link.getToNode().getId().index()];
		int linkIdx = linkIndices[link.getId().index()];

		int base = linkIdx * SpeedyGraph.LINK_SIZE;
		this.linkData[base + 2] = fromNodeIdx;
		this.linkData[base + 3] = toNodeIdx;

		setOutLink(fromNodeIdx, linkIdx);
		setInLink(toNodeIdx, linkIdx);

		this.links[linkIdx] = link;
	}

	private void addLink(TurnRestrictionsContext.ColoredLink link) {
		int fromNodeIdx = -1;
		int toNodeIdx = -1;
		int linkIdx = linkIndices[link.index];

		if (link.fromColoredNode != null) {
			fromNodeIdx = nodeIndices[link.fromColoredNode.index()];
		}
		if (link.fromNode != null) {
			fromNodeIdx = nodeIndices[link.fromNode.getId().index()];
		}
		if (link.toColoredNode != null) {
			toNodeIdx = nodeIndices[link.toColoredNode.index()];
		}
		if (link.toNode != null) {
			toNodeIdx = nodeIndices[link.toNode.getId().index()];
		}

		int base = linkIdx * SpeedyGraph.LINK_SIZE;
		this.linkData[base + 2] = fromNodeIdx;
		this.linkData[base + 3] = toNodeIdx;

		setOutLink(fromNodeIdx, linkIdx);
		setInLink(toNodeIdx, linkIdx);

		this.links[linkIdx] = link.link;
	}

	private void setOutLink(int fromNodeIdx, int linkIdx) {
		final int nodeI = fromNodeIdx * SpeedyGraph.NODE_SIZE;
		int outLinkIdx = this.nodeData[nodeI];
		if (outLinkIdx < 0) {
			this.nodeData[nodeI] = linkIdx;
			return;
		}
		int lastLinkIdx;
		do {
			lastLinkIdx = outLinkIdx;
			outLinkIdx = this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE];
		} while (outLinkIdx >= 0);
		this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE] = linkIdx;
	}

	private void setInLink(int toNodeIdx, int linkIdx) {
		final int nodeI = toNodeIdx * SpeedyGraph.NODE_SIZE + 1;
		int inLinkIdx = this.nodeData[nodeI];
		if (inLinkIdx < 0) {
			this.nodeData[nodeI] = linkIdx;
			return;
		}
		int lastLinkIdx;
		do {
			lastLinkIdx = inLinkIdx;
			inLinkIdx = this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE + 1];
		} while (inLinkIdx >= 0);
		this.linkData[lastLinkIdx * SpeedyGraph.LINK_SIZE + 1] = linkIdx;
	}
}
