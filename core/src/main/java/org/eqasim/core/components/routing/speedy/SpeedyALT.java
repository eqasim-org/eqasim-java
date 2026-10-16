/* Adapted from MATSim 2026.0-2025w33, https://github.com/matsim-org/matsim-libs.
 * Original authors: Marcel Rieser / Simunto (and contributors named below).
 * Modified for Eqasim on 2026-10-15: compact storage and bounded preprocessing.
 * Distributed under the GNU GPL v2 or later, without warranty; see /LICENSE.
 */
package org.eqasim.core.components.routing.speedy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;

import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.*;

/**
 * A very fast implementation of the ALT algorithm (A*-search with Landmarks and Triangle Inequality).
 *
 * Based on "Computing the Shortest Path: A* Search Meets Graph Theory" by Andrew V. Goldberg and Chris Harrelson, 2005.
 *
 * This implementation always looks at all landmarks and does not filter them, as performance measurements
 * suggest that selecting and re-selecting landmarks regularly actually results in an overhead compared
 * to just calculate the values for each landmark in each step (using a typical value of 16 landmarks).
 * This might be due to the fact that all values for each landmark are just next to each other in the memory,
 * so when accessing the travelcosts to/from one landmark basically already loads the values of all landmarks in
 * the CPU cache, making the calculation for the remaining landmarks very fast.
 *
 * This implementation is not thread-safe. In the case of multi-threading, every thread should use
 * a separate instance. (But the used {@link SpeedyALTData} is thread-safe and can be shared by multiple
 * instances).
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
public class SpeedyALT implements LeastCostPathCalculator {

	private final static Logger LOG = LogManager.getLogger(SpeedyALT.class);

	private final SpeedyGraph graph;
	private final SpeedyALTData astarData;
	private final TravelTime tt;
	private final TravelDisutility td;
	private final SearchWorkspace workspace;
	private final double[] destinationFrom, destinationTo;
	private long queries;
	private int lastExpanded, lastQueuePeak, maxVisited, maxExpanded, maxQueuePeak;
	private final SpeedyGraph.LinkIterator outLI;
	private final DAryMinHeap pq;

	public SpeedyALT(SpeedyALTData astarData, TravelTime tt, TravelDisutility td) {
		this.graph = astarData.graph;
		this.astarData = astarData;
		this.tt = tt;
		this.td = td;
		this.workspace = new SearchWorkspace(this.graph.nodeCount);
		this.destinationFrom = new double[astarData.getLandmarksCount()];
		this.destinationTo = new double[astarData.getLandmarksCount()];
		this.pq = new DAryMinHeap(this.graph.nodeCount, 6, workspace::slot, true);
		this.outLI = this.graph.getOutLinkIterator();
	}

	public double getCost(int nodeIndex) {
		return workspace.cost(nodeIndex);
	}

	private double getTimeRaw(int nodeIndex) {
		return workspace.time(nodeIndex);
	}


	private void setData(int nodeIndex, double cost, double time) {
		workspace.set(nodeIndex, cost, time);
	}

	/** Query counters and primitive retained storage; does not include shared graph/landmarks. */
	public SearchStatistics getSearchStatistics() {
		return new SearchStatistics(queries, workspace.visited(), maxVisited, lastExpanded, maxExpanded,
				lastQueuePeak, maxQueuePeak, workspace.capacity(), workspace.dense(),
				workspace.bytes() + pq.bytes() + 8L * (destinationFrom.length + destinationTo.length));
	}

	public record SearchStatistics(long queries, int lastVisited, int maxVisited, int lastExpanded,
			int maxExpanded, int lastQueuePeak, int maxQueuePeak, int recordCapacity, boolean dense, long primitiveBytes) { }

	public Path calcLeastCostPath(Link fromLink, Link toLink, double starttime, final Person person, final Vehicle vehicle) {

		int startNodeIndex = graph.getStartNodeIndex(fromLink);
		int endNodeIndex = graph.getNodeIndex(toLink.getFromNode());

		Path path = calcLeastCostPathImpl(startNodeIndex, endNodeIndex, starttime, person, vehicle);
		if(path == null) {
			LOG.warn("No route was found from link " + fromLink.getId() + " to link " + toLink.getId() + ". Some possible reasons:");
		  LOG.warn("  * Network is not connected.  Run NetworkUtils.cleanNetwork(Network network, Set<String> modes).") ;
			LOG.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
			LOG.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
			LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		}
		return path;
	}

	@Override
	public Path calcLeastCostPath(Node startNode, Node endNode, double startTime, Person person, Vehicle vehicle) {
		Path path = calcLeastCostPathImpl(graph.getNodeIndex(startNode), graph.getNodeIndex(endNode), startTime, person, vehicle);
		if(path == null) {
      LOG.warn("No route was found from node " + startNode.getId() + " to node " + endNode.getId() + ". Some possible reasons:");
		  LOG.warn("  * Network is not connected.  Run NetworkUtils.cleanNetwork(Network network, Set<String> modes).") ;
		  LOG.warn("  * Network for considered mode does not even exist.  Modes need to be entered for each link in network.xml.");
		  LOG.warn("  * Network for considered mode is not connected to starting or ending point of route.  Setting insertingAccessEgressWalk to true may help.");
		  LOG.warn("This will now return null, but it may fail later with a NullPointerException.");
		}
		return path;
	}

	private Path calcLeastCostPathImpl(int startNodeIndex, int endNodeIndex, double startTime, Person person, Vehicle vehicle) {
		pq.clear();
		if (workspace.beginQuery()) pq.useDensePositions();
		queries++;
		int expanded = 0, queuePeak = 1;
		boolean hasTurnRestrictions = this.graph.hasTurnRestrictions();

		int startDeadend = this.astarData.getNodeDeadend(startNodeIndex);
		int endDeadend = this.astarData.getNodeDeadend(endNodeIndex);

		for (int i = 0; i < destinationFrom.length; i++) {
			destinationFrom[i] = astarData.getTravelCostFromLandmark(endNodeIndex, i);
			destinationTo[i] = astarData.getTravelCostToLandmark(endNodeIndex, i);
		}
		double estimation = astarData.estimate(startNodeIndex, destinationFrom, destinationTo);

		setData(startNodeIndex, 0, startTime);
		workspace.incoming(startNodeIndex, -1);
		this.pq.insert(startNodeIndex, 0 + estimation);
		boolean foundEndNode = false;

		while (!this.pq.isEmpty()) {
			final int nodeIdx = this.pq.poll();
			if (nodeIdx == endNodeIndex) {
				foundEndNode = true;
				break;
			}
			// if turn restrictions are used, we might be on a colored node, so check for the original node
			if (hasTurnRestrictions && this.graph.getOriginalNodeIndex(nodeIdx) == endNodeIndex) {
				foundEndNode = true;
				endNodeIndex = nodeIdx;
				break;
			}

			// ignore dead-ends
			int deadend = this.astarData.getNodeDeadend(nodeIdx);
			if (deadend >= 0 && deadend != startDeadend && deadend != endDeadend) {
				continue; // it's a dead-end we're not interested in
			}
			expanded++;

			double currTime = getTimeRaw(nodeIdx);
			double currCost = getCost(nodeIdx);

			this.outLI.reset(nodeIdx);
			while (this.outLI.next()) {
				int linkIdx = this.outLI.getLinkIndex();
				Link link = this.graph.getLink(linkIdx);
				int toNode = this.outLI.getToNodeIndex();

				double travelTime = this.tt.getLinkTravelTime(link, currTime, person, vehicle);
				double newTime = currTime + travelTime;
				double travelCost = this.td.getLinkTravelDisutility(link, currTime, person, vehicle);
				double newCost = currCost + travelCost;

				if (workspace.contains(toNode)) {
					// this node was already visited in this route-query
					double oldCost = getCost(toNode);
					if (newCost < oldCost) {
						estimation = astarData.estimate(toNode, destinationFrom, destinationTo);
						this.pq.decreaseKey(toNode, newCost + estimation);
						setData(toNode, newCost, newTime);
						workspace.incoming(toNode, linkIdx);
					}
				} else {
					estimation = astarData.estimate(toNode, destinationFrom, destinationTo);
					setData(toNode, newCost, newTime);
					this.pq.insert(toNode, newCost + estimation);
					workspace.incoming(toNode, linkIdx);
				}
			}
			queuePeak = Math.max(queuePeak, pq.size());
		}
		lastExpanded = expanded;
		lastQueuePeak = queuePeak;
		maxVisited = Math.max(maxVisited, workspace.visited());
		maxExpanded = Math.max(maxExpanded, lastExpanded);
		maxQueuePeak = Math.max(maxQueuePeak, lastQueuePeak);

		if (foundEndNode) {
			return constructPath(endNodeIndex, startTime);
		}
		return null;
	}


	private Path constructPath(int endNodeIndex, double startTime) {
		double travelCost = getCost(endNodeIndex);
		double arrivalTime = getTimeRaw(endNodeIndex);
		if (Double.isInfinite(arrivalTime)) {
			throw new RuntimeException("Undefined time on end node");
		}
		double travelTime = arrivalTime - startTime;

		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();

		int nodeIndex = endNodeIndex;

		nodes.add(this.graph.getNode(nodeIndex));

		int linkIndex = workspace.incoming(nodeIndex);
        while (linkIndex >= 0) {
            nodeIndex = graph.getFromNodeIndex(linkIndex);
            nodes.add(graph.getNode(nodeIndex));
            links.add(graph.getLink(linkIndex));
            linkIndex = workspace.incoming(nodeIndex);
        }

		Collections.reverse(nodes);
		Collections.reverse(links);

		return new Path(nodes, links, travelTime, travelCost);
	}

}
