/* Adapted from MATSim 2026.0-2025w33, https://github.com/matsim-org/matsim-libs.
 * Original authors: Marcel Rieser / Simunto (and contributors named below).
 * Modified for Eqasim on 2026-10-15: compact storage and bounded preprocessing.
 * Distributed under the GNU GPL v2 or later, without warranty; see /LICENSE.
 */
package org.eqasim.core.components.routing.speedy;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import java.util.Arrays;



/**
 * Implements a highly optimized data structure for representing a MATSim network. Optimized to use as little memory as possible, and thus to fit as much memory as possible into CPU caches for high
 * performance.
 * <p>
 * Inspired by GraphHopper's Graph data structure as described in https://github.com/graphhopper/graphhopper/blob/master/docs/core/technical.md. GraphHopper uses bi-directional links, while MATSim
 * uses uni-directional links. Thus, instead of having nodeA and nodeB, we always have from- and to-node. This implies that one `node-row` requires two link-ids: one for the first in-link, and one for
 * the first out-link.
 * <p>
 * We use simple int-arrays (int[]) to store the data. This should provide fast and thread-safe read-only access, but limits the number of nodes and links in the network to (Integer.MAX_VALUE/2 =
 * 1.073.741.823) nodes and (Integer.MAX_VALUE/4 = 536.870.911) links. I hope that for the foreseeable future, these limits are high enough.
 * <p>
 * This class is thread-safe, allowing a single graph to be used by multiple threads.
 *
 * @author mrieser
 */
public class SpeedyGraph {

    // Compact primitive storage: 2 ints per routing node and 4 per routing
    // link, plus original-object references and compact endpoint mappings.
    // No TurnRestrictionsContext or its builder objects survive construction.

    final static int NODE_SIZE = 2;
    final static int LINK_SIZE = 4;

    final int nodeCount;
    final int linkCount;
    private final int[] nodeData;
    private final int[] linkData;
    private final Link[] links;
    private final Node[] nodes;
    final int originalNodeCount;
    final int legacyNodeSlots;
    private final int[] originalNodeIds;
    private final int[] coloredOriginalIndices;
    private final int[] restrictedLinkIds;
    private final int[] restrictedStartNodes;
    private final boolean turnRestrictions;
    private final int[] nextEquivalentNode;

	SpeedyGraph(int[] nodeData, int[] linkData, Node[] nodes, Link[] links,
                int originalNodeCount, int legacyNodeSlots, int[] originalNodeIds, int[] coloredOriginalIndices,
                int[] restrictedLinkIds, int[] restrictedStartNodes, boolean turnRestrictions) {
        this.nodeData = nodeData;
        this.linkData = linkData;
        this.nodes = nodes;
        this.links = links;
        this.nodeCount = nodes.length;
        this.linkCount = links.length;
        this.originalNodeCount = originalNodeCount;
        this.legacyNodeSlots = legacyNodeSlots;
        this.originalNodeIds = originalNodeIds;
        this.coloredOriginalIndices = coloredOriginalIndices;
        this.restrictedLinkIds = restrictedLinkIds;
        this.restrictedStartNodes = restrictedStartNodes;
        this.turnRestrictions = turnRestrictions;
        // A destination has several approach states when turns are restricted.
        // Share this compact lookup across routers rather than scanning every
        // colored node, or allocating a destination list, for each query.
        this.nextEquivalentNode = turnRestrictions ? new int[nodes.length] : new int[0];
        Arrays.fill(nextEquivalentNode, -1);
        for (int i = originalNodeCount; i < nodes.length; i++) {
            int original = coloredOriginalIndices[i - originalNodeCount];
            nextEquivalentNode[i] = nextEquivalentNode[original];
            nextEquivalentNode[original] = i;
        }
    }

    int getNodeIndex(Node node) {
        int index = Arrays.binarySearch(originalNodeIds, node.getId().index());
        if (index < 0) throw new IllegalArgumentException("Node is outside this routing graph: " + node.getId());
        return index;
    }

    int getOriginalNodeIndex(int internal) {
        return internal < originalNodeCount ? internal : coloredOriginalIndices[internal - originalNodeCount];
    }

    int getFromNodeIndex(int linkIndex) { return linkData[linkIndex * LINK_SIZE + 2]; }

    int getStartNodeIndex(Link link) {
        int position = Arrays.binarySearch(restrictedLinkIds, link.getId().index());
        return position < 0 ? getNodeIndex(link.getToNode()) : restrictedStartNodes[position];
    }

    boolean hasTurnRestrictions() { return turnRestrictions; }

    int getNextEquivalentNode(int nodeIndex) { return nextEquivalentNode[nodeIndex]; }

    /** Whether this approach permits the final transition into an activity link. */
    boolean canEnterLink(int nodeIndex, Link destination) {
        // Colored nodes retain the complete restriction history, including
        // multi-link restrictions. Test their actual outgoing edges rather
        // than only the last physical link of the reconstructed route.
        for (int index = nodeData[nodeIndex * NODE_SIZE]; index >= 0;
                index = linkData[index * LINK_SIZE]) {
            if (links[index].getId().equals(destination.getId())) return true;
        }
        return false;
    }

    public LinkIterator getOutLinkIterator() {
        return new OutLinkIterator(this);
    }

    public LinkIterator getInLinkIterator() {
        return new InLinkIterator(this);
    }

    Link getLink(int index) {
        return this.links[index];
    }

    Node getNode(int index) {
        return this.nodes[index];
    }


    public interface LinkIterator {

        void reset(int nodeIdx);

        boolean next();

        int getLinkIndex();

        int getToNodeIndex();

        int getFromNodeIndex();

    }

    private static abstract class AbstractLinkIterator implements LinkIterator {

        final SpeedyGraph graph;
        int nodeIdx = -1;
        int linkIdx = -1;

        AbstractLinkIterator(SpeedyGraph graph) {
            this.graph = graph;
        }

        @Override
        final public void reset(int nodeIdx) {
            this.nodeIdx = nodeIdx;
            this.linkIdx = -1;
        }

        @Override
        abstract public boolean next();

        @Override
        final public int getLinkIndex() {
            return this.linkIdx;
        }

        @Override
        final public int getFromNodeIndex() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 2];
        }

        @Override
        final public int getToNodeIndex() {
            return this.graph.linkData[this.linkIdx * LINK_SIZE + 3];
        }

    }

    private static class OutLinkIterator extends AbstractLinkIterator {

        OutLinkIterator(SpeedyGraph graph) {
            super(graph);
        }

        @Override
        public boolean next() {
            if (this.nodeIdx < 0) {
                return false;
            }
            if (this.linkIdx < 0) {
                this.linkIdx = graph.nodeData[this.nodeIdx * NODE_SIZE];
            } else {
                this.linkIdx = graph.linkData[this.linkIdx * LINK_SIZE];
            }
            if (this.linkIdx < 0) {
                this.nodeIdx = -1;
                return false;
            }
            return true;
        }
    }

    private static class InLinkIterator extends AbstractLinkIterator {

        InLinkIterator(SpeedyGraph graph) {
            super(graph);
        }

        @Override
        public boolean next() {
            if (this.nodeIdx < 0) {
                return false;
            }
            if (this.linkIdx < 0) {
                this.linkIdx = graph.nodeData[this.nodeIdx * NODE_SIZE + 1];
            } else {
                this.linkIdx = graph.linkData[this.linkIdx * LINK_SIZE + 1];
            }
            if (this.linkIdx < 0) {
                this.nodeIdx = -1;
                return false;
            }
            return true;
        }
    }

}
