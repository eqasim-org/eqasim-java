package org.eqasim.core.components.routing.speedy;

import java.util.Arrays;

/** Private, reusable query state. Node IDs in the graph/heap never change.
 * Small searches use packed records; broad searches switch to dense records
 * between queries, when no live predecessor/queue state needs conversion.
 */
final class SearchWorkspace {
    private final int nodeCount;
    private final int[] index;
    private double[] data;
    private int[] incoming;
    private int[] nodes;
    private int size;
    private int generation;
    private boolean dense;

    SearchWorkspace(int nodeCount) {
        this.nodeCount = nodeCount;
        index = new int[nodeCount];
        int initial = Math.min(nodeCount, 64);
        data = new double[initial * 2];
        incoming = new int[initial];
        nodes = new int[initial];
    }

    /** Returns true when the heap must replace its packed position mapping. */
    boolean beginQuery() {
        if (!dense && incoming.length >= nodeCount * 0.875) {
            dense = true;
            if (incoming.length != nodeCount) {
                data = new double[nodeCount * 2];
                incoming = new int[nodeCount];
            }
            nodes = null;
            Arrays.fill(index, 0);
            generation = 1;
            size = 0;
            return true;
        }
        if (dense) {
            if (++generation == 0) {
                Arrays.fill(index, 0);
                generation = 1;
            }
        } else {
            for (int slot = 0; slot < size; slot++) index[nodes[slot]] = 0;
        }
        size = 0;
        return false;
    }

    boolean contains(int node) { return dense ? index[node] == generation : index[node] != 0; }

    int slot(int node) { return dense ? node : index[node] - 1; }

    double cost(int node) { return data[slot(node) * 2]; }
    double time(int node) { return data[slot(node) * 2 + 1]; }
    int incoming(int node) { return incoming[slot(node)]; }
    void incoming(int node, int link) { incoming[slot(node)] = link; }

    void set(int node, double cost, double time) {
        int slot;
        if (dense) {
            slot = node;
            if (index[node] != generation) {
                index[node] = generation;
                size++;
            }
        } else {
            slot = index[node] - 1;
            if (slot < 0) {
                slot = size++;
                if (slot == incoming.length) {
                    int capacity = Math.min(nodeCount, Math.max(slot + 1, slot + slot / 2));
                    data = Arrays.copyOf(data, capacity * 2);
                    incoming = Arrays.copyOf(incoming, capacity);
                    nodes = Arrays.copyOf(nodes, capacity);
                }
                nodes[slot] = node;
                index[node] = slot + 1;
            }
        }
        data[slot * 2] = cost;
        data[slot * 2 + 1] = time;
    }

    int visited() { return size; }
    int capacity() { return incoming.length; }
    boolean dense() { return dense; }
    long bytes() { return 4L * index.length + 8L * data.length + 4L * incoming.length
            + (nodes == null ? 0 : 4L * nodes.length); }
}
