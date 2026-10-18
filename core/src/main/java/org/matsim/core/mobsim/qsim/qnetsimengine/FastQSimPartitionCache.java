package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Singleton;
import java.lang.ref.WeakReference;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;

/** Controller-scoped immutable assignments: avoid rescanning national plans every iteration. */
@Singleton
public final class FastQSimPartitionCache {
    private int parts;
    private long fingerprint;
    private int[] assignment;
    private double[] measuredWeights;
    private WeakReference<Scenario> cachedScenario = new WeakReference<>(null);

    synchronized int[] get(Node[] nodes, Scenario scenario, int requestedParts) {
        long hash = scenario.getNetwork().getLinks().size();
        hash = 31 * hash + scenario.getConfig().qsim().getMainModes().hashCode();
        for (Node node : nodes) {
            hash = 31 * hash + node.getId().index();
            hash = 31 * hash + Double.doubleToLongBits(node.getCoord().getX());
            hash = 31 * hash + Double.doubleToLongBits(node.getCoord().getY());
            hash = 31 * hash + node.getOutLinks().size();
            for (var link : node.getOutLinks().values()) {
                hash = 31 * hash + link.getId().index();
                hash = 31 * hash + link.getToNode().getId().index();
            }
        }
        if (assignment == null || cachedScenario.get() != scenario || parts != requestedParts
                || fingerprint != hash || assignment.length != nodes.length) {
            measuredWeights = null;
            assignment = TrafficWeightedPartitioner.partition(nodes, scenario, requestedParts);
            fingerprint = hash;
            parts = requestedParts;
            cachedScenario = new WeakReference<>(scenario);
        }
        else if (measuredWeights != null) {
            assignment = TrafficWeightedPartitioner.partition(nodes, measuredWeights, requestedParts);
            measuredWeights = null;
        }
        // Private to this package; engines only read the returned array.
        return assignment;
    }

    /** Feedback is consumed only by the next QSim initialization, never during a phase. */
    synchronized void recordWork(Scenario scenario, int[] previousAssignment, Node[] nodes, double[] work) {
        if (cachedScenario.get() != scenario || assignment != previousAssignment || work.length != assignment.length) return;
        double total = 0;
        for (double value : work) {
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException("Invalid measured work");
            total += value;
        }
        if (total == 0) return;
        measuredWeights = new double[work.length];
        for (int i = 0; i < work.length; i++) {
            // Keep a baseline for cold nodes. Normalize timings so clock scale
            // cannot change their relative importance; isolated pauses are capped at collection.
            measuredWeights[i] = 0.1 * (1 + 0.125 * nodes[i].getOutLinks().size()) + 0.9 * work[i] * work.length / total;
        }
    }
}
