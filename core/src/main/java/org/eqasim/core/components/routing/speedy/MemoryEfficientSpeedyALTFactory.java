/* Adapted from MATSim 2026.0-2025w33, https://github.com/matsim-org/matsim-libs.
 * Original authors: Marcel Rieser / Simunto (and contributors named below).
 * Modified for Eqasim on 2026-10-15: compact storage and bounded preprocessing.
 * Distributed under the GNU GPL v2 or later, without warranty; see /LICENSE.
 */
package org.eqasim.core.components.routing.speedy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * SpeedyALT with compact storage, preserving MATSim 2026.0-2025w33 search order.
 * One factory per injector; graph/landmarks are shared, search workspaces are not.
 * Topology and minimum link costs must remain stable for the factory's lifetime,
 * as with the original factory. Actual time-dependent costs remain live.
 *
 * @author mrieser / Simunto (original SpeedyALTFactory)
 */
public final class MemoryEfficientSpeedyALTFactory implements LeastCostPathCalculatorFactory {
    private final int landmarksCount;
    private final Map<Network, PreparedGraph> graphs = new IdentityHashMap<>();

    public MemoryEfficientSpeedyALTFactory() { this(16); }

    /** Alternate counts are useful for benchmarking; normal simulation routing keeps 16. */
    public MemoryEfficientSpeedyALTFactory(int landmarksCount) {
        if (landmarksCount < 1) throw new IllegalArgumentException("Landmark count must be positive");
        this.landmarksCount = landmarksCount;
    }

    private static final class PreparedGraph {
        final SpeedyGraph graph;
        final List<SpeedyALTData> landmarks = new ArrayList<>();
        PreparedGraph(Network network) { graph = SpeedyGraphBuilder.build(network, null); }
    }

    // Atomic publication also prevents concurrent duplicate graph/preprocessing
    // allocations. Searches themselves run independently without this lock.
    @Override
    public synchronized LeastCostPathCalculator createPathCalculator(Network network,
            TravelDisutility travelCosts, TravelTime travelTimes) {
        PreparedGraph prepared = graphs.computeIfAbsent(network, PreparedGraph::new);
        SpeedyALTData matching = null;
        for (var data : prepared.landmarks) {
            if (data.hasSameMinimumCosts(travelCosts)) { matching = data; break; }
        }
        if (matching == null) {
            // Preserve the original landmark count even when dense indexing removes
            // unused slots on very small networks (the original may repeat landmarks).
            matching = new SpeedyALTData(prepared.graph, Math.min(landmarksCount, prepared.graph.legacyNodeSlots), travelCosts);
            prepared.landmarks.add(matching);
        }
        return new SpeedyALT(matching, travelTimes, travelCosts);
    }

    /** Diagnostics without exposing mutable graphs or preprocessing tables. */
    public synchronized List<Statistics> getStatistics() {
        return graphs.values().stream().map(p -> new Statistics(p.graph.legacyNodeSlots,
                p.graph.nodeCount, p.graph.linkCount, p.landmarks.size(),
                p.landmarks.stream().mapToLong(SpeedyALTData::landmarkBytes).sum(),
                p.landmarks.stream().mapToLong(SpeedyALTData::uncompressedLandmarkBytes).sum(),
                Math.min(landmarksCount, p.graph.legacyNodeSlots))).toList();
    }

    public record Statistics(int legacyNodeSlots, int routingNodes, int routingLinks, int landmarkSets,
            long landmarkBytes, long uncompressedLandmarkBytes, int landmarksPerSet) { }
}
