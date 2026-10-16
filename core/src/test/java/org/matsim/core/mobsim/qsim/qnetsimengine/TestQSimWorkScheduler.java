package org.matsim.core.mobsim.qsim.qnetsimengine;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestQSimWorkScheduler {
    @Test(timeout = 15_000) public void idleDelayedWorkerDoesNotHoldUpFinishedPhases() throws Exception {
        var releaseWorker = new CountDownLatch(1);
        var phasesFinished = new CountDownLatch(1);
        var rescued = new AtomicBoolean();
        var created = new AtomicInteger();
        var scheduler = new QSimWorkScheduler(3, task -> {
            boolean delayed = created.getAndIncrement() == 0;
            return new Thread(() -> {
                if (delayed) {
                    try { releaseWorker.await(); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                }
                task.run();
            });
        });
        var rescue = new Thread(() -> {
            try { rescued.set(!phasesFinished.await(3, TimeUnit.SECONDS)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { releaseWorker.countDown(); }
        });
        rescue.setDaemon(true); rescue.start();
        try {
            var calls = new AtomicInteger();
            Runnable[] tasks = {calls::incrementAndGet, calls::incrementAndGet};
            // Only worker 0 is requested, but it has not entered its loop yet.
            // The coordinator can finish both blocks and advance safely anyway.
            for (int i = 0; i < 50; i++) scheduler.execute(tasks, 2);
            phasesFinished.countDown();
            rescue.join();
            assertFalse("Finished phases waited for an unused worker", rescued.get());
            for (int i = 0; i < 2000; i++) scheduler.execute(tasks, 2);
            assertEquals(4100, calls.get());
        } finally {
            phasesFinished.countDown(); releaseWorker.countDown(); scheduler.close(); rescue.join();
        }
    }

    @Test(timeout = 20_000) public void phasesPublishStateAndExecuteEveryBlockExactlyOnce() {
        for (int threads : new int[] {1, 2, 4}) {
            try (var scheduler = new QSimWorkScheduler(threads)) {
                int[] state = new int[47];
                int[] expected = new int[47];
                Runnable[] tasks = new Runnable[state.length];
                for (int i = 0; i < tasks.length; i++) {
                    int slot = i;
                    tasks[i] = () -> state[slot]++;
                }
                for (int phase = 1; phase <= 2000; phase++) {
                    int count = 1 + phase % tasks.length;
                    scheduler.execute(tasks, count, (phase & 15) == 0, 1 + phase % threads);
                    for (int i = 0; i < state.length; i++) {
                        if (i < count) expected[i]++;
                        assertEquals(expected[i], state[i]);
                    }
                    scheduler.execute(tasks, 0);
                }
                scheduler.execute(tasks, 1);
                assertEquals(expected[0] + 1, state[0]);
            }
        }
    }

    @Test(timeout = 10_000) public void workerFailuresPropagateAndWorkersCanBeClosed() {
        try (var scheduler = new QSimWorkScheduler(4)) {
            RuntimeException failure = new RuntimeException("test worker error");
            try {
                scheduler.execute(new Runnable[] {() -> { throw failure; }, () -> {}, () -> {}}, 3);
                fail("Worker failure was swallowed");
            } catch (RuntimeException error) { assertSame(failure, error); }
            var calls = new AtomicInteger();
            scheduler.execute(new Runnable[] {calls::incrementAndGet, calls::incrementAndGet}, 2);
            assertEquals(2, calls.get());
        }
    }

    @Test(timeout = 10_000) public void interruptedCoordinatorStillWaitsForAllMutations() {
        try (var scheduler = new QSimWorkScheduler(3)) {
            var calls = new AtomicInteger();
            Thread.currentThread().interrupt();
            try {
                scheduler.execute(new Runnable[] {calls::incrementAndGet, calls::incrementAndGet}, 2);
                fail("Expected interruption");
            } catch (IllegalStateException expected) {
                assertEquals(2, calls.get());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally { Thread.interrupted(); }
        }
    }
}
