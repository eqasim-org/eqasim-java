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
    private WeakReference<Scenario> cachedScenario = new WeakReference<>(null);

    synchronized int[] get(Node[] nodes, Scenario scenario, int requestedParts) {
        long hash = scenario.getNetwork().getLinks().size();
        hash = 31 * hash + scenario.getConfig().qsim().getMainModes().hashCode();
        for (Node node : nodes) {
            hash = 31 * hash + node.getId().index();
            hash = 31 * hash + Double.doubleToLongBits(node.getCoord().getX());
            hash = 31 * hash + Double.doubleToLongBits(node.getCoord().getY());
            hash = 31 * hash + node.getOutLinks().size();
        }
        if (assignment == null || cachedScenario.get() != scenario || parts != requestedParts
                || fingerprint != hash || assignment.length != nodes.length) {
            assignment = TrafficWeightedPartitioner.partition(nodes, scenario, requestedParts);
            fingerprint = hash;
            parts = requestedParts;
            cachedScenario = new WeakReference<>(scenario);
        }
        // Private to this package; engines only read the returned array.
        return assignment;
    }
}
