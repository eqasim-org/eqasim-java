package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/** Lane-local FIFO with primitive flow consumption. No concurrent access within a QSim phase. */
abstract class QVehicleBuffer implements Iterable<QVehicle> {
    static final String PROPERTY = "eqasim.qsim.compactBuffers";

    static QVehicleBuffer create(boolean compact) { return compact ? new Circular() : new Legacy(); }
    abstract void add(QVehicle vehicle, double flowConsumption);
    abstract QVehicle peek();
    abstract QVehicle poll();
    abstract int size();
    abstract void clear();
    abstract double remainingFlowCapacity(double capacity);
    final boolean isEmpty() { return size() == 0; }

    /** Storage is allocated only after a lane first offers a vehicle. */
    private static final class Circular extends QVehicleBuffer {
        private QVehicle[] vehicles = new QVehicle[2];
        private double[] consumptions = new double[2];
        private int head, size;

        @Override void add(QVehicle vehicle, double flowConsumption) {
            if (size == vehicles.length) grow();
            int tail = index(size);
            vehicles[tail] = vehicle;
            consumptions[tail] = flowConsumption;
            size++;
        }

        private int index(int offset) {
            int index = head + offset;
            return index >= vehicles.length ? index - vehicles.length : index;
        }

        private void grow() {
            int capacity = Math.multiplyExact(vehicles.length, 2);
            var nextVehicles = new QVehicle[capacity];
            var nextConsumptions = new double[capacity];
            int first = vehicles.length - head;
            System.arraycopy(vehicles, head, nextVehicles, 0, first);
            System.arraycopy(consumptions, head, nextConsumptions, 0, first);
            System.arraycopy(vehicles, 0, nextVehicles, first, head);
            System.arraycopy(consumptions, 0, nextConsumptions, first, head);
            vehicles = nextVehicles;
            consumptions = nextConsumptions;
            head = 0;
        }

        @Override QVehicle peek() { return size == 0 ? null : vehicles[head]; }

        @Override QVehicle poll() {
            if (size == 0) return null;
            QVehicle vehicle = vehicles[head];
            vehicles[head] = null;
            consumptions[head] = 0;
            if (++head == vehicles.length) head = 0;
            if (--size == 0) head = 0;
            return vehicle;
        }

        @Override int size() { return size; }

        @Override void clear() {
            Arrays.fill(vehicles, null);
            Arrays.fill(consumptions, 0);
            head = size = 0;
        }

        @Override double remainingFlowCapacity(double capacity) {
            // Retain native FIFO subtraction order and double precision. Keeping
            // a running sum instead would change rounding and capacity decisions.
            int index = head;
            for (int i = 0; i < size; i++) {
                capacity -= consumptions[index];
                if (++index == consumptions.length) index = 0;
            }
            return capacity;
        }

        @Override public Iterator<QVehicle> iterator() {
            return new Iterator<>() {
                private int offset;
                @Override public boolean hasNext() { return offset < size; }
                @Override public QVehicle next() {
                    if (!hasNext()) throw new NoSuchElementException();
                    return vehicles[index(offset++)];
                }
            };
        }
    }

    /** Original storage retained for validation and a runtime fallback. */
    private static final class Legacy extends QVehicleBuffer {
        private final LinkedList<Pair<QVehicle, Double>> queue = new LinkedList<>();
        @Override void add(QVehicle vehicle, double consumption) { queue.add(new ImmutablePair<>(vehicle, consumption)); }
        @Override QVehicle peek() { return queue.isEmpty() ? null : queue.peek().getKey(); }
        @Override QVehicle poll() { return queue.isEmpty() ? null : queue.poll().getKey(); }
        @Override int size() { return queue.size(); }
        @Override void clear() { queue.clear(); }
        @Override double remainingFlowCapacity(double capacity) {
            for (var entry : queue) capacity -= entry.getValue();
            return capacity;
        }
        @Override public Iterator<QVehicle> iterator() {
            var entries = queue.iterator();
            return new Iterator<>() {
                @Override public boolean hasNext() { return entries.hasNext(); }
                @Override public QVehicle next() { return entries.next().getKey(); }
            };
        }
    }
}
