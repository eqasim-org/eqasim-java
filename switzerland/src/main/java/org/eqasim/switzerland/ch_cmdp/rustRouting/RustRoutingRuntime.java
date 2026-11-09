// Owns asynchronous native timetable preparation and releases it after the controller finishes.
package org.eqasim.switzerland.ch_cmdp.rustRouting;

import org.apache.logging.log4j.*;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.router.TransitScheduleChangedEvent;

import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;

public final class RustRoutingRuntime
        implements AutoCloseable, StartupListener, ShutdownListener, IterationEndsListener, BasicEventHandler {
    private static final Logger LOG = LogManager.getLogger(RustRoutingRuntime.class);

    record Prepared(long handle, RustTimetableSnapshot snapshot) {}

    private final CompletableFuture<Prepared> preparation;
    private final AtomicBoolean closed = new AtomicBoolean();
    final RustRoutingConfigGroup settings;
    // Striped counters avoid serializing MATSim's concurrent routing workers.
    private final LongAdder nativeQueries = new LongAdder();
    private final LongAdder nativeNanos = new LongAdder();
    private final LongAdder noPaths = new LongAdder();
    private final LongAdder expansions = new LongAdder();
    private final LongAdder budgetLimited = new LongAdder();
    record CoordinateResult(double[] data, boolean truncated) {}

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
                                LOG.info("Rust coordinate routing: {}", NativePtRouter.hasCoordinates ? "native" : "legacy Java endpoints");
                                LOG.info("Rust coordinate heuristics supported: {}; budget={}, wait trigger={} s, journey trigger={} s, quality radius={} m",
                                        NativePtRouter.hasCoordinatePolicy, settings.coordinateWorkBudget,
                                        settings.qualityWaitSeconds, settings.qualityJourneySeconds, settings.qualityRadiusMeters);
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

    /** Eager readiness barrier for long-lived tools that do not create a simulation controller. */
    public void awaitReady() { prepared(); }

    /** Fresh adapter per worker; all adapters share this runtime's immutable timetable. */
    public org.matsim.pt.router.TransitRouter createTransitRouter() {
        var adapter = new RustPtRoutingModule(this, null);
        return adapter::transitLegs;
    }

    Prepared prepared() {
        if (closed.get()) throw new IllegalStateException("Rust PT router is closed");
        try {
            return preparation.join();
        } catch (CompletionException e) {
            throw new IllegalStateException("Rust PT preprocessing failed", e.getCause());
        }
    }

    /** Measures only the native call; aggregate time is parallel worker time, not elapsed time. */
    double[] routeNative(long handle, long departure, int boardings, long duration,
            int[] access, int[] egress) {
        long started = System.nanoTime();
        try {
            double[] result = NativePtRouter.route(handle, departure, boardings, duration, access, egress);
            if (result.length == 0) noPaths.increment();
            return result;
        } finally {
            nativeQueries.increment();
            nativeNanos.add(System.nanoTime() - started);
        }
    }

    /** Coordinate searches and retries run in Rust on the calling MATSim worker. */
    double[] routeCoordinates(long handle, org.matsim.core.router.RoutingRequest request) {
        return routeCoordinatesResult(handle, request).data();
    }
    CoordinateResult routeCoordinatesResult(long handle, org.matsim.core.router.RoutingRequest request) {
        if (!NativePtRouter.hasCoordinates)
            return new CoordinateResult(LegacyRustEndpoints.route(prepared().snapshot(), this, handle, request), false);
        var from = request.getFromFacility().getCoord();
        var to = request.getToFacility().getCoord();
        long started = System.nanoTime();
        try {
            boolean detailed = NativePtRouter.hasCoordinatePolicy;
            double[] data = detailed ? NativePtRouter.routeCoordinatesDetailed(handle,
                    NativePtRouter.millis(request.getDepartureTime()), settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), from.getX(), from.getY(), to.getX(), to.getY())
                    : NativePtRouter.routeCoordinates(handle,
                    NativePtRouter.millis(request.getDepartureTime()), settings.maxBoardings,
                    NativePtRouter.millis(settings.maxDurationSeconds), from.getX(), from.getY(), to.getX(), to.getY());
            int header = detailed ? 2 : 1;
            boolean limited = detailed && data[1] != 0;
            expansions.add((long) data[0] - 1);
            if (limited) budgetLimited.increment();
            if (data.length == header) noPaths.increment();
            return new CoordinateResult(java.util.Arrays.copyOfRange(data, header, data.length), limited);
        } finally {
            nativeQueries.increment();
            nativeNanos.add(System.nanoTime() - started);
        }
    }

    void recordExpansion() { expansions.increment(); }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        long queries = nativeQueries.sumThenReset();
        long nanos = nativeNanos.sumThenReset();
        LOG.info("Rust PT iteration {}: {} native queries, {} empty results, {} expanded searches; "
                        + "mean native call {} ms, summed parallel worker time {} s, budget-limited results {}",
                event.getIteration(), queries, noPaths.sumThenReset(), expansions.sumThenReset(),
                queries == 0 ? 0.0 : nanos / (queries * 1e6), nanos / 1e9, budgetLimited.sumThenReset());
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
