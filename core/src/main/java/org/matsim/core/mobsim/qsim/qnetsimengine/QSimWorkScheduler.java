package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/** Reusable phase scheduler. The coordinator participates, so workers + coordinator = configured threads. */
final class QSimWorkScheduler implements AutoCloseable {
    private final Worker[] workers;
    static final int MAX_TASKS = 0xffff;
    // [32-bit generation | 16-bit task count | 16-bit next task]. A late worker
    // cannot claim another generation's work, even after the task array is reused.
    // Separate padded claim counters avoid one global hot CAS location.
    private final AtomicLongArray cursors;
    private final Runnable[][] queues;
    private final int[] counts;
    private final long[] busy;
    private final int[] completed;
    private final boolean local = Boolean.parseBoolean(System.getProperty("eqasim.qsim.localScheduling", "true"));
    private static final int STRIDE = 16;
    interface LocalTask extends Runnable { int preferredWorker(); }
    private double coordinationNanos;
    private double lastCoordinationNanos;
    private int participants;
    private long localClaims, stolenClaims;
    private final long[] steals;
    private final AtomicInteger remaining = new AtomicInteger();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final Thread coordinator = Thread.currentThread();
    private volatile boolean awaiting;

    private boolean measure;
    private final AtomicLong sampledBusyNanos = new AtomicLong();
    private long sampledAvailableNanos;
    private long epoch;
    private volatile boolean closed;
    private final int spinIterations;

    QSimWorkScheduler(int threads) {
        this(threads, Thread::new);
    }

    QSimWorkScheduler(int threads, ThreadFactory threadFactory) {
        if (threads < 1) throw new IllegalArgumentException("QSim needs at least one thread");
        spinIterations = Integer.getInteger("eqasim.qsim.workerSpins", 512);
        if (spinIterations < 0) throw new IllegalArgumentException("workerSpins must be nonnegative");
        cursors = new AtomicLongArray(threads * STRIDE);
        queues = new Runnable[threads][];
        counts = new int[threads];
        busy = new long[threads * STRIDE];
        completed = new int[threads * STRIDE];
        steals = new long[threads * STRIDE];
        workers = new Worker[threads - 1];
        for (int i = 0; i < workers.length; i++) workers[i] = new Worker(i, threadFactory);
        for (Worker worker : workers) worker.thread.start();
    }

    void execute(Runnable[] tasks, int count) {
        execute(tasks, count, false);
    }

    void execute(Runnable[] tasks, int count, boolean measure) {
        execute(tasks, count, measure, workers.length + 1);
    }

    void execute(Runnable[] tasks, int count, boolean measure, int parallelism) {
        if (Thread.currentThread() != coordinator) throw new IllegalStateException("QSim phases need one coordinator");
        if (parallelism < 1) throw new IllegalArgumentException("Parallelism must be positive");
        if (closed) throw new IllegalStateException("QSim workers are closed");
        if (count < 0 || count > MAX_TASKS || count > tasks.length) throw new IllegalArgumentException("Invalid task count: " + count);

        if (count == 0) return;
        if (count == 1 || workers.length == 0 || parallelism == 1) {
            for (int i = 0; i < count; i++) tasks[i].run();
            return;
        }
        boolean interrupted = Thread.interrupted();
        this.measure = measure;
        long start = measure ? System.nanoTime() : 0;
        long current = ++epoch;
        if (current > 0xffff_ffffL) throw new IllegalStateException("Too many QSim phases");
        participants = Math.min(Math.min(workers.length + 1, parallelism), count);
        Arrays.fill(counts, 0);
        for (int i = 0; i < count; i++) counts[owner(tasks[i], i)]++;
        for (int i = 0; i < participants; i++) {
            if (queues[i] == null || queues[i].length < counts[i]) queues[i] = new Runnable[counts[i]];
        }
        Arrays.fill(counts, 0);
        for (int i = 0; i < count; i++) {
            int owner = owner(tasks[i], i);
            queues[owner][counts[owner]++] = tasks[i];
        }
        remaining.set(count);
        for (int i = 0; i < participants; i++) {
            busy[i * STRIDE] = 0;
            completed[i * STRIDE] = 0;
            steals[i * STRIDE] = 0;
            cursors.set(i * STRIDE, (current << 32) | ((long) counts[i] << 16));
        }
        for (int i = 0; i < participants - 1; i++) {
            Worker worker = workers[i];
            worker.requested = current; // Publishes tasks, count, runner time and phase.
            if (worker.parked) LockSupport.unpark(worker.thread);
        }
        process(current, 0);
        int spins = 0;
        while (remaining.get() != 0) {
            if (Thread.interrupted()) interrupted = true;
            if (spins++ < spinIterations) Thread.onSpinWait();
            else {
                awaiting = true;
                if (remaining.get() != 0) LockSupport.park(this);
                awaiting = false;
            }
        }
        if (measure) {
            long elapsed = System.nanoTime() - start;
            sampledAvailableNanos += elapsed * participants;
            long longest = 0, total = 0;
            for (int i = 0; i < participants; i++) {
                longest = Math.max(longest, busy[i * STRIDE]);
                total += busy[i * STRIDE];
                stolenClaims += steals[i * STRIDE];
                localClaims += completed[i * STRIDE] - steals[i * STRIDE];
            }
            sampledBusyNanos.addAndGet(total);
            // Time not accounted for by the busiest participant estimates phase
            // publication, wake-up, stealing and barrier overhead. Clamp OS pauses.
            double overhead = Math.min(200_000, Math.max(0, elapsed - longest));
            lastCoordinationNanos = overhead;
            coordinationNanos = coordinationNanos == 0 ? overhead : 0.95 * coordinationNanos + 0.05 * overhead;
        }
        for (int i = 0; i < participants; i++) Arrays.fill(queues[i], 0, counts[i], null);
        Throwable error = failure.getAndSet(null);
        if (interrupted) Thread.currentThread().interrupt();
        if (error instanceof Error e) throw e;
        if (error instanceof RuntimeException e) throw e;
        if (error != null) throw new RuntimeException("QSim worker failed", error);
        if (interrupted) throw new IllegalStateException("Interrupted while waiting for QSim workers");
    }

    double sampledOccupancy() {
        return sampledAvailableNanos == 0 ? Double.NaN : (double) sampledBusyNanos.get() / sampledAvailableNanos;
    }

    private int owner(Runnable task, int index) {
        if (!local) return 0;
        if (task instanceof LocalTask localTask) {
            int preferred = localTask.preferredWorker();
            if (preferred < 0 || preferred > workers.length) throw new IllegalArgumentException("Invalid preferred worker");
            return preferred * participants / (workers.length + 1);
        }
        return index % participants;
    }

    double coordinationNanos() { return coordinationNanos; }
    double lastCoordinationNanos() { return lastCoordinationNanos; }
    double stolenFraction() {
        return localClaims + stolenClaims == 0 ? Double.NaN : (double) stolenClaims / (localClaims + stolenClaims);
    }

    private void process(long generation, int worker) {
        // Read a fixed bound only. A delayed worker may observe a newer phase,
        // but cannot claim it: every queue carries its own generation tag.
        int available = participants;
        int source = worker < available ? worker : 0;
        int finished = 0;
        try {
            while (true) {
                long ticket = cursors.get(source * STRIDE);
                if ((ticket >>> 32) != generation) return;
                int index = (int) (ticket & MAX_TASKS);
                int count = (int) ((ticket >>> 16) & MAX_TASKS);
                if (index >= count) {
                    // Prefer nearby spatial groups; steal whole unstarted blocks.
                    int found = -1;
                    for (int offset = 1; offset < available; offset++) {
                        int candidate = (worker + offset) % available;
                        long other = cursors.get(candidate * STRIDE);
                        if ((other >>> 32) == generation && (other & MAX_TASKS) < ((other >>> 16) & MAX_TASKS)) {
                            found = candidate; break;
                        }
                    }
                    if (found < 0) return;
                    source = found;
                    continue;
                }
                if (!cursors.compareAndSet(source * STRIDE, ticket, ticket + 1)) continue;
                // A successful claim pins this phase until remaining is decremented.
                boolean sampled = measure;
                long started = sampled ? System.nanoTime() : 0;
                try {
                    if (failure.get() == null) queues[source][index].run();
                } catch (Throwable error) {
                    failure.compareAndSet(null, error);
                } finally {
                    if (sampled) {
                        busy[worker * STRIDE] += System.nanoTime() - started;
                        completed[worker * STRIDE]++;
                        if (source != worker) steals[worker * STRIDE]++;
                    }
                    finished++;
                }
            }
        } finally {
            // Publish all completed blocks together. Claimed blocks keep the
            // phase pinned until here; workers that never claim work need no
            // acknowledgement. This removes a shared atomic write per block.
            if (finished != 0 && remaining.addAndGet(-finished) == 0 && awaiting)
                LockSupport.unpark(coordinator);
        }
    }

    @Override public void close() {
        closed = true;
        for (int i = 0; i < queues.length; i++) cursors.set(i * STRIDE, 0);
        for (Worker worker : workers) LockSupport.unpark(worker.thread);
        boolean interrupted = false;
        for (Worker worker : workers) {
            while (worker.thread.isAlive()) {
                try { worker.thread.join(); }
                catch (InterruptedException e) { interrupted = true; }
            }
        }
        Arrays.fill(queues, null);
        if (interrupted) Thread.currentThread().interrupt();
    }

    private final class Worker implements Runnable {
        private final Thread thread;
        private final int index;
        private volatile long requested;
        private volatile boolean parked;

        Worker(int index, ThreadFactory threadFactory) {
            this.index = index + 1;
            thread = threadFactory.newThread(this);
            thread.setName("QSimFast-" + index);
            thread.setDaemon(true); // A controller failure must not strand a JVM.
        }

        @Override public void run() {
            long observed = 0;
            int spins = 0;
            while (!closed) {
                long current = requested;
                if (current != observed) {
                    process(current, index);
                    observed = current;
                    spins = 0;
                } else if (spins++ < spinIterations) {
                    Thread.onSpinWait();
                } else {
                    parked = true;
                    // Recheck after publishing parked: avoids losing a coordinator wake-up.
                    if (requested == observed && !closed) LockSupport.park(this);
                    parked = false;
                    spins = 0;
                }
            }
        }
    }
}
