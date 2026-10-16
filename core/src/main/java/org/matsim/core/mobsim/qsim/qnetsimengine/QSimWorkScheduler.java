package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;

/** Reusable phase scheduler. The coordinator participates, so workers + coordinator = configured threads. */
final class QSimWorkScheduler implements AutoCloseable {
    private final Worker[] workers;
    static final int MAX_TASKS = 0xffff;
    // [32-bit generation | 16-bit task count | 16-bit next task]. A late worker
    // cannot claim another generation's work, even after the task array is reused.
    private final AtomicLong cursor = new AtomicLong();
    private final AtomicInteger remaining = new AtomicInteger();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final Thread coordinator = Thread.currentThread();
    private volatile boolean awaiting;
    private Runnable[] tasks;
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
        cursor.set(0); // Invalidate requests from an already completed phase.
        if (count == 0) return;
        if (count == 1 || workers.length == 0 || parallelism == 1) {
            for (int i = 0; i < count; i++) tasks[i].run();
            return;
        }
        boolean interrupted = Thread.interrupted();
        this.tasks = tasks;
        this.measure = measure;
        long start = measure ? System.nanoTime() : 0;
        long current = ++epoch;
        if (current > 0xffff_ffffL) throw new IllegalStateException("Too many QSim phases");
        int participants = Math.min(Math.min(workers.length, parallelism - 1), count - 1);
        remaining.set(count);
        cursor.set((current << 32) | ((long) count << 16));
        for (int i = 0; i < participants; i++) {
            Worker worker = workers[i];
            worker.requested = current; // Publishes tasks, count, runner time and phase.
            if (worker.parked) LockSupport.unpark(worker.thread);
        }
        process(current);
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
            sampledAvailableNanos += (System.nanoTime() - start) * (participants + 1);
        }
        this.tasks = null; // Do not retain QSim instances after a phase.
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

    private void process(long generation) {
        while (true) {
            long ticket = cursor.get();
            if ((ticket >>> 32) != generation) return;
            int index = (int) (ticket & MAX_TASKS);
            int count = (int) ((ticket >>> 16) & MAX_TASKS);
            if (index >= count) return;
            if (!cursor.compareAndSet(ticket, ticket + 1)) continue;
            // A successful claim pins this phase: remaining includes this task
            // until all its reads/writes finish. Only now read the shared fields.
            boolean sampled = measure;
            long start = sampled ? System.nanoTime() : 0;
            try {
                if (failure.get() == null) tasks[index].run();
            } catch (Throwable error) {
                failure.compareAndSet(null, error);
            } finally {
                if (sampled) sampledBusyNanos.addAndGet(System.nanoTime() - start);
                // Failed phases drain unstarted tasks too. Only the last task
                // wakes the coordinator; idle workers need not acknowledge.
                if (remaining.decrementAndGet() == 0 && awaiting) LockSupport.unpark(coordinator);
            }
        }
    }

    @Override public void close() {
        closed = true;
        cursor.set(0);
        for (Worker worker : workers) LockSupport.unpark(worker.thread);
        boolean interrupted = false;
        for (Worker worker : workers) {
            while (worker.thread.isAlive()) {
                try { worker.thread.join(); }
                catch (InterruptedException e) { interrupted = true; }
            }
        }
        tasks = null;
        if (interrupted) Thread.currentThread().interrupt();
    }

    private final class Worker implements Runnable {
        private final Thread thread;
        private volatile long requested;
        private volatile boolean parked;

        Worker(int index, ThreadFactory threadFactory) {
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
                    process(current);
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
