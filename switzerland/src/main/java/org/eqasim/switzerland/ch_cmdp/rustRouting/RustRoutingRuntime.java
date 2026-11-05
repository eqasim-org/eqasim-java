// Owns asynchronous native timetable preparation and releases it after the controller finishes.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.apache.logging.log4j.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.router.TransitScheduleChangedEvent;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RustRoutingRuntime
        implements AutoCloseable, StartupListener, ShutdownListener, BasicEventHandler {
    private static final Logger LOG = LogManager.getLogger(RustRoutingRuntime.class);

    record Prepared(long handle, RustTimetableSnapshot snapshot) {}

    private final CompletableFuture<Prepared> preparation;
    private final AtomicBoolean closed = new AtomicBoolean();
    final RustRoutingConfigGroup settings;

    /** Start after scenario/network adaptations; the worker overlaps controller construction. */
    public RustRoutingRuntime(Scenario scenario, RustRoutingConfigGroup settings) {
        this.settings = settings;
        RustRoutingModule.validate(scenario.getConfig(), settings);
        var executor =
                Executors.newSingleThreadExecutor(
                        r -> {
                            Thread thread = new Thread(r, "rust-pt-preprocessing");
                            thread.setDaemon(true);
                            return thread;
                        });
        preparation =
                CompletableFuture.supplyAsync(
                        () -> {
                            Path file = null;
                            long start = System.nanoTime();
                            try {
                                // Acquisition/compilation is also asynchronous on the first run.
                                NativePtRouter.load(scenario.getConfig(), settings);
                                LOG.info(
                                        "Preparing shared Rust PT timetable from the loaded"
                                                + " scenario");
                                var snapshot = new RustTimetableSnapshot(scenario);
                                file = Files.createTempFile("eqasim-rust-timetable-", ".bin");
                                snapshot.write(file);
                                long handle =
                                        NativePtRouter.open(file.toString(), snapshot.costsJson());
                                LOG.info(
                                        "Rust PT router ready: {} stops, {} departures, {} s;"
                                                + " existing MATSim workers share the timetable",
                                        snapshot.stops.length,
                                        snapshot.trips.size(),
                                        (System.nanoTime() - start) / 1e9);
                                return new Prepared(handle, snapshot);
                            } catch (Exception e) {
                                throw new CompletionException(
                                        "Cannot initialize Rust PT routing", e);
                            } finally {
                                if (file != null)
                                    try {
                                        Files.deleteIfExists(file);
                                    } catch (Exception e) {
                                        LOG.warn("Cannot delete temporary timetable {}", file, e);
                                    }
                            }
                        },
                        executor);
        executor.shutdown();
    }

    Prepared prepared() {
        if (closed.get()) throw new IllegalStateException("Rust PT router is closed");
        try {
            return preparation.join();
        } catch (CompletionException e) {
            throw new IllegalStateException("Rust PT preprocessing failed", e.getCause());
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event instanceof TransitScheduleChangedEvent)
            throw new IllegalStateException(
                    "Rust PT routing requires a static schedule; disable it when modifying the"
                            + " timetable during simulation");
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        close();
    }

    @Override
    public void notifyStartup(StartupEvent event) {
        prepared();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        // Always release a handle even when shutdown races the background preparation.
        preparation.whenComplete(
                (ready, failure) -> {
                    if (ready != null) NativePtRouter.close(ready.handle());
                    else
                        LOG.debug(
                                "Failed PT initialization has no native handle to release",
                                failure);
                });
    }
}
