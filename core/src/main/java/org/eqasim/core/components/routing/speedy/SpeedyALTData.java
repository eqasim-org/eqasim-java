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
import org.eqasim.core.components.routing.speedy.SpeedyGraph.LinkIterator;
import org.matsim.core.router.util.TravelDisutility;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Preprocessed data for the ALT algorithm, see {@link SpeedyALT}.
 *
 * This class is thread-safe and can safely be used by multiple threads.
 *
 * @author mrieser / Simunto, sponsored by SBB Swiss Federal Railways
 */
class SpeedyALTData {

	private final static Logger LOG = LogManager.getLogger(SpeedyALTData.class);

	final SpeedyGraph graph;
	private final int landmarksCount;
	private final TravelDisutility travelCosts;
	private final int[] landmarksNodeIndices;
	private final LandmarkTable fromLandmarks;
	private final LandmarkTable toLandmarks;
	private final int[] deadendData;
	private final double minTravelCostPerLength;

	public SpeedyALTData(SpeedyGraph graph, int landmarksCount, TravelDisutility travelCosts) {
		this.graph = graph;
		this.landmarksCount = landmarksCount;
		this.travelCosts = travelCosts;
		this.landmarksNodeIndices = new int[landmarksCount];
		this.fromLandmarks = new LandmarkTable(graph.nodeCount, landmarksCount);
		this.toLandmarks = new LandmarkTable(graph.nodeCount, landmarksCount);
		this.deadendData = new int[graph.nodeCount];

		this.findDeadEnds();
		this.calcLandmarks();
		this.fromLandmarks.compact();
		this.toLandmarks.compact();
		LOG.info("Landmark storage: from_rows={} to_rows={} routing_nodes={}; {} / {} primitive bytes",
				fromLandmarks.storedRows(), toLandmarks.storedRows(), graph.nodeCount,
				landmarkBytes(), uncompressedLandmarkBytes());
		this.minTravelCostPerLength = this.calcMinTravelCostPerLength();
	}

    boolean hasSameMinimumCosts(TravelDisutility candidate) {
        if (candidate == travelCosts) return true;
        // Network identity alone is insufficient when the same graph is requested
        // with different lower-bound costs (e.g. another mode or routing policy).
        for (int i = 0; i < graph.linkCount; i++) {
            Link link = graph.getLink(i);
            if (Double.doubleToLongBits(candidate.getLinkMinimumTravelDisutility(link))
                    != Double.doubleToLongBits(travelCosts.getLinkMinimumTravelDisutility(link))) return false;
        }
        return true;
    }

	private void findDeadEnds() {
		LOG.info("find dead ends...");

		LinkIterator outLI = this.graph.getOutLinkIterator();
		LinkIterator inLI = this.graph.getInLinkIterator();
		Arrays.fill(this.deadendData, -1);
		Map<Integer, Integer> mergedDeadends = new HashMap<>();

		for (int nodeIdx = 0; nodeIdx < this.graph.nodeCount; nodeIdx++) {
			Node node = this.graph.getNode(nodeIdx);
			if (node == null) continue; // not all indices might be in use

			if (this.deadendData[nodeIdx] >= 0) continue; // already detected as part of dead-end

			int nIdx = nodeIdx;
			int otherNodeIndex = checkNodeForDeadend(this.deadendData, mergedDeadends, nIdx, nodeIdx, outLI, inLI);
			while (otherNodeIndex >= 0) {
				this.deadendData[nIdx] = nodeIdx;
				nIdx = otherNodeIndex;
				otherNodeIndex = checkNodeForDeadend(this.deadendData, mergedDeadends, nIdx, nodeIdx, outLI, inLI);
			}
		}
		Map<Integer, Integer> mergers = new HashMap<>();
		mergedDeadends.forEach((fromIdx, toIdx) -> {
			int finalToIdx = toIdx;
			Integer newToIdx = mergedDeadends.get(toIdx);
			while (newToIdx != null && newToIdx != finalToIdx) {
				finalToIdx = newToIdx;
				newToIdx = mergedDeadends.get(finalToIdx);
			}
			mergers.put(fromIdx, finalToIdx);
		});
		for (int nodeIdx = 0; nodeIdx < this.graph.nodeCount; nodeIdx++) {
			int deadend = this.deadendData[nodeIdx];
			if (deadend >= 0) {
				this.deadendData[nodeIdx] = mergers.getOrDefault(deadend, deadend);
			}
		}
	}

	private int checkNodeForDeadend(int[] deadends, Map<Integer, Integer> mergedDeadends, int nodeIdx, int currentDeadend, LinkIterator outLI, LinkIterator inLI) {
		int otherNodeIndex = -1;

		outLI.reset(nodeIdx);
		while (outLI.next()) {
			int toNodeIdx = outLI.getToNodeIndex();
			if (deadends[toNodeIdx] >= 0) continue;

			if (toNodeIdx != otherNodeIndex) {
				if (otherNodeIndex == -1) otherNodeIndex = toNodeIdx;
				else return -1; // there are more than one non-dead-end incident nodes
			}
		}

		inLI.reset(nodeIdx);
		while (inLI.next()) {
			int fromNodeIdx = inLI.getFromNodeIndex();
			if (deadends[fromNodeIdx] >= 0) continue;

			if (fromNodeIdx != otherNodeIndex) {
				if (otherNodeIndex == -1) otherNodeIndex = fromNodeIdx;
				else return -1; // there are more than one non-dead-end incident nodes
			}
		}

		outLI.reset(nodeIdx);
		while (outLI.next()) {
			int toNodeIdx = outLI.getToNodeIndex();
			int deadend = deadends[toNodeIdx];
			if (deadend >= 0) mergedDeadends.put(deadend, currentDeadend);
		}
		inLI.reset(nodeIdx);
		while (inLI.next()) {
			int fromNodeIdx = inLI.getFromNodeIndex();
			int deadend = deadends[fromNodeIdx];
			if (deadend >= 0) mergedDeadends.put(deadend, currentDeadend);
		}

		return otherNodeIndex;
	}

	private void calcLandmarks() {
		LOG.info("calculate landmarks...");
		Node firstNode = null;
		for (int i = 0; i < this.graph.nodeCount; i++) {
			firstNode = this.graph.getNode(i);
			if (firstNode != null) {
				break;
			}
		}
		if (firstNode == null) {
			LOG.warn("Network does not contain any nodes!");
			return;
		}

		var trees = new java.util.ArrayList<Future<?>>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        // Each worker owns one reusable distance array and heap. Futures contain
        // no trees, so completed work cannot retain 32 full-sized double arrays.
        ThreadLocal<Scratch> scratch = ThreadLocal.withInitial(() -> new Scratch(graph.nodeCount, landmarksCount));
        Scratch selection = new Scratch(graph.nodeCount, landmarksCount);
        try {
            int firstLandmarkIndex = graph.getNodeIndex(firstNode);
            this.landmarksNodeIndices[0] = firstLandmarkIndex;
            for (int i = 0; i < landmarksCount; i++) {
                final int node = i == 0 ? firstLandmarkIndex : calculateNextLandmark(i, selection);
                this.landmarksNodeIndices[i] = node;
                final int offset = i * 2;
                trees.add(executor.submit(() -> {
                    Scratch work = scratch.get();
                    calculateTreeForward(node, work);
                    setNodeData(work.data, offset);
                }));
                trees.add(executor.submit(() -> {
                    Scratch work = scratch.get();
                    calculateTreeBackward(node, work);
                    setNodeData(work.data, offset + 1);
                }));
            }
            for (Future<?> tree : trees) tree.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while building ALT landmarks", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Could not build ALT landmarks", e.getCause());
        } finally {
            executor.shutdownNow();
            boolean interrupted = Thread.interrupted();
            try {
                if (!executor.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES)) {
                    throw new IllegalStateException("ALT preprocessing workers did not terminate");
                }
            } catch (InterruptedException e) {
                interrupted = true;
                throw new IllegalStateException("Interrupted while stopping ALT preprocessing", e);
            } finally {
                if (interrupted) Thread.currentThread().interrupt();
            }
        }
    }

    private static final class Scratch {
        final double[] data;
        final NodeMinHeap queue;
        Scratch(int size, int minimumQueueCapacity) {
            data = new double[size];
            queue = new NodeMinHeap(Math.max(size, minimumQueueCapacity), i -> data[i], (i, cost) -> data[i] = cost);
        }
    }

	private double calcMinTravelCostPerLength() {
		LOG.info("calculate min travelcost...");
		double minCost = Double.POSITIVE_INFINITY;
		for (int linkIdx = 0; linkIdx < graph.linkCount; linkIdx++) {
			Link link = this.graph.getLink(linkIdx);
			if (link != null) {
				double cost = this.travelCosts.getLinkMinimumTravelDisutility(link) / link.getLength();
				if (cost < minCost) {
					minCost = cost;
				}
			}
		}
		return minCost;
	}

	private void setNodeData(double[] data, int offset) {
		LandmarkTable table = (offset & 1) == 0 ? fromLandmarks : toLandmarks;
		for (int i = 0; i < this.graph.nodeCount; i++) {
			table.set(i, offset / 2, data[i]);
		}
	}

	private int calculateNextLandmark(int existingCount, Scratch work) {
		double[] data = work.data;
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator outLI = this.graph.getOutLinkIterator();

		for (int i = 0; i < existingCount; i++) {
			data[this.landmarksNodeIndices[i]] = 0;
		}

		NodeMinHeap pq = work.queue;
        pq.clear();
		for (int i = 0; i < existingCount; i++) {
			pq.insert(this.landmarksNodeIndices[i]);
		}

		int lastNodeIdx = -1;
		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			lastNodeIdx = nodeIdx;
			double currCost = data[nodeIdx];

			outLI.reset(nodeIdx);
			while (outLI.next()) {
				int toNode = outLI.getToNodeIndex();

				double newCost = currCost + 1;

				double oldCost = data[toNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(toNode, newCost);
					}
				} else {
					data[toNode] = newCost;
					pq.insert(toNode);
				}
			}
		}
		return lastNodeIdx;
	}

	private void calculateTreeForward(int node, Scratch work) {
		double[] data = work.data;
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator outLI = this.graph.getOutLinkIterator();

		data[node] = 0;

		NodeMinHeap pq = work.queue;
        pq.clear();
		pq.insert(node);

		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			double currCost = data[nodeIdx];

			outLI.reset(nodeIdx);
			while (outLI.next()) {
				int toNode = outLI.getToNodeIndex();

				double newCost = currCost + this.travelCosts.getLinkMinimumTravelDisutility(this.graph.getLink(outLI.getLinkIndex()));

				double oldCost = data[toNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(toNode, newCost);
					}
				} else {
					data[toNode] = newCost;
					pq.insert(toNode);
				}
			}
		}

		if(graph.hasTurnRestrictions()) {
			consolidateColoredNodes(data);
		}

	}

	private void calculateTreeBackward(int node, Scratch work) {
		double[] data = work.data;
		Arrays.fill(data, Double.POSITIVE_INFINITY);
		LinkIterator inLI = this.graph.getInLinkIterator();

		data[node] = 0;

		NodeMinHeap pq = work.queue;
        pq.clear();
		pq.insert(node);

		while (!pq.isEmpty()) {
			final int nodeIdx = pq.poll();
			double currCost = data[nodeIdx];

			inLI.reset(nodeIdx);
			while (inLI.next()) {
				int fromNode = inLI.getFromNodeIndex();

				double newCost = currCost + this.travelCosts.getLinkMinimumTravelDisutility(this.graph.getLink(inLI.getLinkIndex()));

				double oldCost = data[fromNode];
				if (Double.isFinite(oldCost)) {
					if (newCost < oldCost) {
						pq.decreaseKey(fromNode, newCost);
					}
				} else {
					data[fromNode] = newCost;
					pq.insert(fromNode);
				}
			}
		}

		if(graph.hasTurnRestrictions()) {
			consolidateColoredNodes(data);
		}

	}

	private void consolidateColoredNodes(double[] data) {
		// update node values with the minimum of their colored copies, if any
		for (int i = graph.originalNodeCount; i < graph.nodeCount; i++) {
            int uncoloredIndex = graph.getOriginalNodeIndex(i);
            double uncoloredCost = data[uncoloredIndex];
            double coloredCost = data[i];
            if (Double.isFinite(uncoloredCost)) {
                if (coloredCost < uncoloredCost) data[uncoloredIndex] = coloredCost;
            } else {
                data[uncoloredIndex] = coloredCost;
            }
        }
    }

	int getNodeDeadend(int nodeIndex) {
		return this.deadendData[nodeIndex];
	}

	int getLandmarksCount() {
		return this.landmarksCount;
	}

	double getTravelCostFromLandmark(int nodeIndex, int landmarkIndex) {
		return this.fromLandmarks.get(nodeIndex, landmarkIndex);
	}

	double getTravelCostToLandmark(int nodeIndex, int landmarkIndex) {
		return this.toLandmarks.get(nodeIndex, landmarkIndex);
	}

	long landmarkBytes() { return fromLandmarks.bytes() + toLandmarks.bytes(); }
	long uncompressedLandmarkBytes() { return fromLandmarks.uncompressedBytes() + toLandmarks.uncompressedBytes(); }

	/** The same ALT expressions, with each row resolved once and target rows cached per query. */
	double estimate(int node, double[] destinationFrom, double[] destinationTo) {
		return estimate(node, destinationFrom, destinationTo, false);
	}

	double estimate(int node, double[] destinationFrom, double[] destinationTo, boolean linkDestination) {
		int fromRow = fromLandmarks.row(node), toRow = toLandmarks.row(node);
		double[] from = fromLandmarks.block(fromRow), to = toLandmarks.block(toRow);
		int fromOffset = fromLandmarks.offset(fromRow), toOffset = toLandmarks.offset(toRow);
		double best = 0;
		// Preprocessing consolidates original nodes with the minimum of their
		// colored copies. Such a forward distance can be too small for this
		// exact search state, so its subtraction is not a safe lower bound.
		// The caller also disables a backward bound whose destination row was
		// consolidated. Node queries retain the legacy expression unchanged.
		boolean useForward = !linkDestination || node >= graph.originalNodeCount
				|| graph.getNextEquivalentNode(node) < 0;
		for (int i = 0; i < landmarksCount; i++) {
			if (!linkDestination) {
				double estimate = Math.max(to[toOffset + i] - destinationTo[i], destinationFrom[i] - from[fromOffset + i]);
				if (estimate > best) best = estimate;
			} else {
				// Evaluate independently: infinity minus infinity is NaN and
				// must not hide the other, finite lower bound.
				double backward = to[toOffset + i] - destinationTo[i];
				if (backward > best) best = backward;
				double forward = useForward ? destinationFrom[i] - from[fromOffset + i] : 0;
				if (forward > best) best = forward;
			}
		}
		return best;
	}

	public double getMinTravelCostPerLength() {
		return this.minTravelCostPerLength;
	}
}
