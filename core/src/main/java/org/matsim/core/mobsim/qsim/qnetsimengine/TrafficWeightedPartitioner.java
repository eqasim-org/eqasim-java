package org.matsim.core.mobsim.qsim.qnetsimengine;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.population.routes.NetworkRoute;

/** Weighted spatial bisection, using primitive work arrays and no native libraries. */
final class TrafficWeightedPartitioner {
    private TrafficWeightedPartitioner() { }

    /** Returned partitions correspond to the input node order. All scratch storage is initialization-only. */
    static int[] partition(Node[] nodes, Scenario scenario, int parts) {
        if (parts < 1 || parts > nodes.length) throw new IllegalArgumentException("Invalid partition count: " + parts);
        return partition(nodes, initialWeights(nodes, scenario), parts);
    }

    private static double[] initialWeights(Node[] nodes, Scenario scenario) {
        double[] weights = new double[nodes.length];
        var positions = new Int2IntOpenHashMap(nodes.length);
        positions.defaultReturnValue(-1);
        for (int i = 0; i < nodes.length; i++) {
            positions.put(nodes[i].getId().index(), i);
            // Cold nodes retain a small initialization/topology weight.
            weights[i] = 1.0 + 0.125 * nodes[i].getOutLinks().size();
        }
        var links = scenario.getNetwork().getLinks();
        var modes = scenario.getConfig().qsim().getMainModes();
        for (var person : scenario.getPopulation().getPersons().values()) {
            var plan = person.getSelectedPlan();
            if (plan == null) continue;
            for (var element : plan.getPlanElements()) {
                if (!(element instanceof Leg leg) || !modes.contains(leg.getMode())
                        || !(leg.getRoute() instanceof NetworkRoute route)) continue;
                var start = links.get(route.getStartLinkId());
                if (start != null) {
                    int p = positions.get(start.getFromNode().getId().index());
                    if (p >= 0) { weights[p]++; }
                }
                for (var id : route.getLinkIds()) {
                    var link = links.get(id);
                    if (link == null) continue;
                    int p = positions.get(link.getFromNode().getId().index());
                    if (p >= 0) { weights[p]++; }
                }
                if (!route.getStartLinkId().equals(route.getEndLinkId())) {
                    var end = links.get(route.getEndLinkId());
                    if (end != null) {
                        int p = positions.get(end.getFromNode().getId().index());
                        if (p >= 0) { weights[p]++; }
                    }
                }
            }
        }
        return weights;
    }

    static int[] partition(Node[] nodes, double[] weights, int parts) {
        if (parts < 1 || parts > nodes.length || weights.length != nodes.length)
            throw new IllegalArgumentException("Invalid partition dimensions");
        int[] order = new int[nodes.length];
        int[] assignment = new int[nodes.length];
        double total = 0;
        for (int i = 0; i < nodes.length; i++) {
            if (!(weights[i] > 0) || !Double.isFinite(weights[i])) throw new IllegalArgumentException("Invalid work weight");
            order[i] = i;
            total += weights[i];
        }
        split(nodes, weights, order, assignment, 0, nodes.length, 0, parts, total);
        if (Boolean.parseBoolean(System.getProperty("eqasim.qsim.refinePartitions", "true")))
            refine(nodes, weights, assignment, parts);
        return assignment;
    }

    /** Local graph refinement: reduce boundary edges without emptying or overloading a block. */
    static void refine(Node[] nodes, double[] weights, int[] assignment, int parts) {
        var positions = new Int2IntOpenHashMap(nodes.length);
        positions.defaultReturnValue(-1);
        double[] load = new double[parts];
        int[] sizes = new int[parts];
        double total = 0, largest = 0;
        for (int i = 0; i < nodes.length; i++) {
            positions.put(nodes[i].getId().index(), i);
            load[assignment[i]] += weights[i]; sizes[assignment[i]]++;
            total += weights[i]; largest = Math.max(largest, weights[i]);
        }
        double limit = Math.max(total / parts * 1.15, largest * 1.15);
        int[] connections = new int[parts];
        int[] touched = new int[parts];
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < nodes.length; i++) {
                int owner = assignment[i], count = 0;
                if (sizes[owner] <= 1) continue;
                for (var link : nodes[i].getInLinks().values()) {
                    int n = positions.get(link.getFromNode().getId().index());
                    if (n >= 0) { int p = assignment[n]; if (connections[p]++ == 0) touched[count++] = p; }
                }
                for (var link : nodes[i].getOutLinks().values()) {
                    int n = positions.get(link.getToNode().getId().index());
                    if (n >= 0) { int p = assignment[n]; if (connections[p]++ == 0) touched[count++] = p; }
                }
                int best = owner;
                for (int j = 0; j < count; j++) {
                    int p = touched[j];
                    if (connections[p] > connections[best] && load[p] + weights[i] <= limit) best = p;
                }
                if (best != owner) {
                    assignment[i] = best;
                    load[owner] -= weights[i]; load[best] += weights[i];
                    sizes[owner]--; sizes[best]++;
                }
                for (int j = 0; j < count; j++) connections[touched[j]] = 0;
            }
        }
    }

    private static void split(Node[] nodes, double[] weights, int[] order, int[] assignment,
            int from, int to, int firstPart, int parts, double total) {
        if (parts == 1) {
            for (int i = from; i < to; i++) assignment[order[i]] = firstPart;
            return;
        }
        double minX = Double.POSITIVE_INFINITY, minY = minX;
        double maxX = Double.NEGATIVE_INFINITY, maxY = maxX;
        for (int i = from; i < to; i++) {
            var c = nodes[order[i]].getCoord();
            minX = Math.min(minX, c.getX()); maxX = Math.max(maxX, c.getX());
            minY = Math.min(minY, c.getY()); maxY = Math.max(maxY, c.getY());
        }
        boolean xAxis = maxX - minX >= maxY - minY;
        IntArrays.quickSort(order, from, to, (a, b) -> {
            var ca = nodes[a].getCoord(); var cb = nodes[b].getCoord();
            int cmp = Double.compare(xAxis ? ca.getX() : ca.getY(), xAxis ? cb.getX() : cb.getY());
            return cmp != 0 ? cmp : Integer.compare(a, b);
        });
        int leftParts = parts / 2, rightParts = parts - leftParts;
        double target = total * leftParts / parts, leftWeight = 0;
        int cut = from;
        do { leftWeight += weights[order[cut++]]; }
        while (cut < to - rightParts && (cut < from + leftParts || leftWeight < target));
        split(nodes, weights, order, assignment, from, cut, firstPart, leftParts, leftWeight);
        split(nodes, weights, order, assignment, cut, to, firstPart + leftParts, rightParts, total - leftWeight);
    }
}
