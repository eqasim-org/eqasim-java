package org.matsim.core.events;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import static org.junit.Assert.*;

public class TestBatchedEventQueues {
    @Test(timeout = 10_000) public void interruptDuringABatchStopsBeforeTheNextEvent() throws Exception {
        String previous = System.getProperty(ProcessEventThread.BATCH_PROPERTY);
        try {
            for (boolean batching : new boolean[]{false, true}) {
                System.setProperty(ProcessEventThread.BATCH_PROPERTY, Boolean.toString(batching));
                var manager = EventsUtils.createEventsManager();
                var observed = new ArrayList<Event>();
                manager.addHandler((BasicEventHandler) event -> {
                    observed.add(event);
                    Thread.currentThread().interrupt();
                });
                var worker = new ProcessEventThread(manager, 100_000);
                for (int i = 0; i < 1000; i++) worker.processEvent(event(i));
                worker.close();
                var failure = new AtomicReference<Throwable>();
                var consumer = new Thread(worker);
                consumer.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
                consumer.start(); consumer.join(2000);
                assertFalse(consumer.isAlive());
                assertEquals(1, observed.size());
                assertTrue(failure.get().getCause() instanceof InterruptedException);
            }
        } finally { restore(previous); }
    }

    @Test(timeout = 60_000) public void parallelManagerRetainsHandlerIsolationAndPerProducerOrderAcrossIterations() throws Exception {
        String previous = System.getProperty(ProcessEventThread.BATCH_PROPERTY);
        try {
            for (boolean batching : new boolean[]{false, true}) {
                System.setProperty(ProcessEventThread.BATCH_PROPERTY, Boolean.toString(batching));
                var manager = new ParallelEventsManagerImpl(4);
                var observations = new ArrayList<List<Event>>();
                var consumerNames = new ArrayList<java.util.Set<String>>();
                for (int h = 0; h < 4; h++) {
                    var observed = new ArrayList<Event>(); observations.add(observed);
                    var names = new java.util.HashSet<String>(); consumerNames.add(names);
                    manager.addHandler((BasicEventHandler) event -> {
                        observed.add(event); names.add(Thread.currentThread().getName());
                    });
                }
                for (int iteration = 0; iteration < 2; iteration++) {
                    observations.forEach(List::clear); consumerNames.forEach(java.util.Set::clear);
                    manager.resetHandlers(iteration); manager.initProcessing();
                    try (var producers = Executors.newFixedThreadPool(4)) {
                        var jobs = new ArrayList<java.util.concurrent.Callable<Void>>();
                        for (int p = 0; p < 4; p++) {
                            int producer = p;
                            jobs.add(() -> {
                                for (int i = 0; i < 30_001; i++) manager.processEvent(event(producer * 1_000_000 + i));
                                return null;
                            });
                        }
                        for (var result : producers.invokeAll(jobs)) result.get();
                    }
                    manager.finishProcessing();
                    for (int h = 0; h < 4; h++) {
                        var observed = observations.get(h);
                        assertEquals(120_004, observed.size());
                        assertEquals(1, consumerNames.get(h).size());
                        int[] expected = new int[4];
                        for (var event : observed) {
                            int id = (int) event.getTime();
                            assertEquals(expected[id / 1_000_000]++, id % 1_000_000);
                        }
                        for (int count : expected) assertEquals(30_001, count);
                    }
                    manager.processEvent(event(9_000_000));
                    for (var observed : observations) assertEquals(120_005, observed.size());
                }
            }
        } finally { restore(previous); }
    }

    @Test(timeout = 60_000) public void concurrentProducersRetainEveryEventInAcceptedOrderAndFlushFinalPartialBatch() throws Exception {
        String previous = System.getProperty(ProcessEventThread.BATCH_PROPERTY);
        try {
            for (boolean batching : new boolean[]{false, true}) {
                System.setProperty(ProcessEventThread.BATCH_PROPERTY, Boolean.toString(batching));
                for (int limit : new int[]{0, 1, 17, 100_000}) {
                    var manager = EventsUtils.createEventsManager();
                    var actual = new ArrayList<Event>();
                    manager.addHandler((BasicEventHandler) event -> actual.add(event));
                    var worker = new ProcessEventThread(manager, limit);
                    var consumer = new Thread(worker);
                    var failure = new AtomicReference<Throwable>();
                    consumer.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
                    consumer.start();
                    var expected = new ArrayList<Event>();
                    try (var producers = Executors.newFixedThreadPool(4)) {
                        var jobs = new ArrayList<java.util.concurrent.Callable<Void>>();
                        for (int p = 0; p < 4; p++) {
                            int producer = p;
                            jobs.add(() -> {
                                for (int i = 0; i < 4001; i++) {
                                    var event = event(producer * 10_000 + i);
                                    synchronized (worker) {
                                        worker.processEvent(event);
                                        expected.add(event);
                                    }
                                }
                                return null;
                            });
                        }
                        for (var result : producers.invokeAll(jobs)) result.get();
                    }
                    worker.close();
                    consumer.join(15_000);
                    assertFalse("Event worker did not finish", consumer.isAlive());
                    assertNull(failure.get());
                    assertEquals(expected, actual);
                }
            }
        } finally { restore(previous); }
    }

    @Test(timeout = 30_000) public void queuedEventsAfterTheEndMarkerRetainNativeRequeueBehavior() throws Exception {
        String previous = System.getProperty(ProcessEventThread.BATCH_PROPERTY);
        try {
            List<Event> reference = null;
            for (boolean batching : new boolean[]{false, true}) {
                System.setProperty(ProcessEventThread.BATCH_PROPERTY, Boolean.toString(batching));
                var manager = EventsUtils.createEventsManager();
                var observed = new ArrayList<Event>();
                var entered = new CountDownLatch(1);
                var release = new CountDownLatch(1);
                manager.addHandler((BasicEventHandler) event -> {
                    if (!(event instanceof LastEventOfIteration)) observed.add(event);
                    if (event.getTime() == 0) {
                        entered.countDown();
                        try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
                        catch (InterruptedException e) { throw new RuntimeException(e); }
                    }
                });
                var worker = new ProcessEventThread(manager, 1);
                var failure = new AtomicReference<Throwable>();
                var consumer = new Thread(worker);
                consumer.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
                worker.processEvent(event(0)); worker.processEvent(event(1));
                consumer.start();
                assertTrue(entered.await(10, TimeUnit.SECONDS));
                worker.close();
                for (int i = 2; i < 18; i++) worker.processEvent(event(i));
                release.countDown();
                consumer.join(10_000);
                assertFalse(consumer.isAlive()); assertNull(failure.get());
                assertEquals(18, observed.size());
                if (reference == null) reference = observed;
                else assertEquals(reference.stream().map(Event::getTime).toList(), observed.stream().map(Event::getTime).toList());
            }
        } finally { restore(previous); }
    }

    @Test(timeout = 10_000) public void interruptionAndHandlerFailureTerminateTheWorker() throws Exception {
        String previous = System.getProperty(ProcessEventThread.BATCH_PROPERTY);
        try {
            System.setProperty(ProcessEventThread.BATCH_PROPERTY, "true");
            var worker = new ProcessEventThread(EventsUtils.createEventsManager(), 3);
            var failure = new AtomicReference<Throwable>();
            var consumer = new Thread(worker);
            consumer.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
            consumer.start(); consumer.interrupt(); consumer.join(2000);
            assertFalse(consumer.isAlive()); assertNotNull(failure.get());
            var manager = EventsUtils.createEventsManager();
            var marker = new IllegalArgumentException("handler failure");
            manager.addHandler((BasicEventHandler) event -> { throw marker; });
            worker = new ProcessEventThread(manager, 0);
            failure.set(null);
            consumer = new Thread(worker);
            consumer.setUncaughtExceptionHandler((thread, error) -> failure.set(error));
            worker.processEvent(event(1)); consumer.start(); consumer.join(2000);
            assertFalse(consumer.isAlive()); assertSame(marker, failure.get());
        } finally { restore(previous); }
    }

    private static Event event(int id) {
        return new LinkEnterEvent(id, Id.createVehicleId(id), Id.createLinkId("road"));
    }

    private static void restore(String value) {
        if (value == null) System.clearProperty(ProcessEventThread.BATCH_PROPERTY);
        else System.setProperty(ProcessEventThread.BATCH_PROPERTY, value);
    }
}
