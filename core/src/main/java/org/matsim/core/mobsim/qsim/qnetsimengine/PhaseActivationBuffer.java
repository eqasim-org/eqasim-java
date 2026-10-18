package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Multiple producers reserve distinct slots; the phase barrier publishes their
 * writes before the single consumer drains. This is NOT a general concurrent
 * queue: consuming concurrently with producers is forbidden. Capacity is the
 * number of nodes owned by the block; a node can have only one pending activation.
 */
final class PhaseActivationBuffer<T> {
    private final Object[] entries;
    private final AtomicInteger size = new AtomicInteger();

    PhaseActivationBuffer(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("Negative activation capacity");
        entries = new Object[capacity];
    }

    void add(T value) {
        if (value == null) throw new NullPointerException("activation");
        int slot = size.getAndIncrement();
        if (slot >= entries.length) throw new IllegalStateException("Duplicate activation or incorrect block ownership");
        entries[slot] = value;
    }

    int size() { return size.get(); }

    @SuppressWarnings("unchecked")
    void drainTo(Consumer<T> consumer) {
        int count = size.get();
        for (int i = 0; i < count; i++) {
            T value = (T) entries[i];
            if (value == null) throw new IllegalStateException("Activation producers have not finished");
            entries[i] = null;
            consumer.accept(value);
        }
        size.set(0);
    }
}
