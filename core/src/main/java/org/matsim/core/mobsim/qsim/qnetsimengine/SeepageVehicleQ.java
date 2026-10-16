package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.PriorityQueue;

import org.matsim.core.mobsim.qsim.qnetsimengine.vehicleq.VehicleQ;

/**
 * Native passing queue with a small index of queued seepage vehicles. QSim owns
 * each lane exclusively during a movement phase, so this index needs no locks.
 * Vehicle modes must remain unchanged while queued, as in the standard QSim.
 */
final class SeepageVehicleQ extends AbstractQueue<QVehicle> implements VehicleQ<QVehicle> {
    private static final Comparator<QVehicle> EXIT_TIME =
            (a, b) -> Double.compare(a.getEarliestLinkExitTime(), b.getEarliestLinkExitTime());

    // One lazily created scratch heap per worker, rather than one per link/query.
    // Clear references after every use; retaining its capacity avoids allocation.
    private static final ThreadLocal<TieBreakHeap> TIE_BREAK_QUEUE =
            ThreadLocal.withInitial(TieBreakHeap::new);

    // Same heap and comparator as native PassingVehicleQ, without another wrapper.
    private final PriorityQueue<QVehicle> delegate = new PriorityQueue<>(11, EXIT_TIME);
    private final Collection<String> seepModes;
    private IdentityHashMap<QVehicle, Boolean> seepVehicles;

    SeepageVehicleQ(Collection<String> seepModes) {
        this.seepModes = seepModes; // QSim config shared by all links, fixed during simulation.
    }

    /** Preserve native heap layout, ordering and time-dependent exit-time reads. */
    @Override public boolean offer(QVehicle vehicle) {
        boolean added = delegate.offer(vehicle);
        if (added && seepModes.contains(vehicle.getDriver().getMode())) {
            if (seepVehicles == null) seepVehicles = new IdentityHashMap<>(2);
            seepVehicles.put(vehicle, Boolean.TRUE);
        }
        return added;
    }

    @Override public void addFirst(QVehicle vehicle) {
        vehicle.setEarliestLinkExitTime(Double.NEGATIVE_INFINITY);
        add(vehicle);
    }

    @Override public QVehicle peek() { return delegate.peek(); }

    @Override public QVehicle poll() {
        QVehicle vehicle = delegate.poll();
        forget(vehicle);
        return vehicle;
    }

    @Override public int size() { return delegate.size(); }

    @Override public boolean remove(Object vehicle) {
        Iterator<QVehicle> iterator = iterator();
        while (iterator.hasNext()) {
            if (Objects.equals(vehicle, iterator.next())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    @Override public void clear() {
        delegate.clear();
        if (seepVehicles != null) seepVehicles.clear();
    }

    @Override public Iterator<QVehicle> iterator() {
        Iterator<QVehicle> iterator = delegate.iterator();
        return new Iterator<>() {
            private QVehicle last;
            @Override public boolean hasNext() { return iterator.hasNext(); }
            @Override public QVehicle next() { return last = iterator.next(); }
            @Override public void remove() {
                iterator.remove();
                forget(last);
            }
        };
    }

    private void forget(QVehicle vehicle) {
        // Arriving drivers may already have changed state or been detached.
        // Remove by vehicle identity, without reading the current driver/mode.
        if (seepVehicles != null) seepVehicles.remove(vehicle);
    }

    QVehicle peekSeepageVehicle(double now) {
        QVehicle head = delegate.peek();
        if (head == null || seepVehicles == null || seepVehicles.isEmpty()) return head;

        QVehicle candidate = null;
        boolean tied = false;
        double earliest = Double.POSITIVE_INFINITY;
        for (QVehicle vehicle : seepVehicles.keySet()) {
            double exit = vehicle.getEarliestLinkExitTime();
            if (!(exit <= now)) continue;
            int comparison = candidate == null ? -1 : Double.compare(exit, earliest);
            if (comparison < 0) {
                candidate = vehicle;
                earliest = exit;
                tied = false;
            } else if (comparison == 0) {
                tied = true;
            }
        }
        if (candidate == null) return head;
        if (!tied) return candidate;

        // Native equal-time selection depends on inserting ALL vehicles in
        // their current heap-iteration order. Preserve it exactly on ties.
        TieBreakHeap scratch = TIE_BREAK_QUEUE.get();
        try {
            for (QVehicle vehicle : delegate) scratch.add(vehicle);
            QVehicle vehicle;
            while ((vehicle = scratch.poll()) != null) {
                if (vehicle.getEarliestLinkExitTime() <= now && seepModes.contains(vehicle.getDriver().getMode())) {
                    return vehicle;
                }
            }
            return head;
        } finally {
            scratch.clear();
        }
    }

    /**
     * Reproduce PriorityQueue's comparator-based sift-up/sift-down and tie
     * ordering. Exit times are constant during this lane-owned lookup; reading
     * each once avoids repeated virtual calls while reconstructing the heap.
     */
    private static final class TieBreakHeap {
        private QVehicle[] vehicles = new QVehicle[16];
        private double[] exits = new double[16];
        private int size;

        void add(QVehicle vehicle) {
            double exit = vehicle.getEarliestLinkExitTime();
            if (size == vehicles.length) {
                int capacity = size + size / 2 + 1;
                vehicles = Arrays.copyOf(vehicles, capacity);
                exits = Arrays.copyOf(exits, capacity);
            }
            int index = size++;
            while (index > 0) {
                int parent = (index - 1) >>> 1;
                if (Double.compare(exit, exits[parent]) >= 0) break;
                vehicles[index] = vehicles[parent];
                exits[index] = exits[parent];
                index = parent;
            }
            vehicles[index] = vehicle;
            exits[index] = exit;
        }

        QVehicle poll() {
            if (size == 0) return null;
            QVehicle result = vehicles[0];
            int last = --size;
            QVehicle vehicle = vehicles[last];
            double exit = exits[last];
            vehicles[last] = null;
            if (last > 0) {
                int index = 0;
                int half = size >>> 1;
                while (index < half) {
                    int child = (index << 1) + 1;
                    int right = child + 1;
                    if (right < size && Double.compare(exits[child], exits[right]) > 0) child = right;
                    if (Double.compare(exit, exits[child]) <= 0) break;
                    vehicles[index] = vehicles[child];
                    exits[index] = exits[child];
                    index = child;
                }
                vehicles[index] = vehicle;
                exits[index] = exit;
            }
            return result;
        }

        void clear() {
            Arrays.fill(vehicles, 0, size, null);
            size = 0;
        }
    }
}
