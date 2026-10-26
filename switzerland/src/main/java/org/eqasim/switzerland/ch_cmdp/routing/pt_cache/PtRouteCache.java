package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.router.TransitScheduleChangedEvent;

/** Shared across workers and iterations. Disk indexes stay on disk, not in an unbounded Java map. */
public final class PtRouteCache implements BasicEventHandler, IterationEndsListener, ShutdownListener, AutoCloseable {
    private static final Logger LOG = LogManager.getLogger(PtRouteCache.class);
    private static final int SHARDS = 16;
    private final Shard[] shards = new Shard[SHARDS];
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean diskDisabled = new AtomicBoolean();
    private final long hotBytesPerShard;
    final ConcurrentHashMap<PtRouteKey, CompletableFuture<byte[]>> inFlight = new ConcurrentHashMap<>();
    final LongAdder requests = new LongAdder(), memoryHits = new LongAdder(), diskHits = new LongAdder();
    final LongAdder misses = new LongAdder(), coalesced = new LongAdder(), bypasses = new LongAdder();
    final LongAdder bytesWritten = new LongAdder();
    private volatile boolean closed;

    /** The caller supplies a new, empty run-specific directory. */
    public PtRouteCache(Path directory, int memoryMb) {
        if (memoryMb < 0) throw new IllegalArgumentException("PT cache memoryMb must be nonnegative");
        hotBytesPerShard = (long) memoryMb * 1024 * 1024 / SHARDS;
        for (int i = 0; i < SHARDS; i++) shards[i] = new Shard();
        try {
            Files.createDirectories(directory);
            for (int i = 0; i < SHARDS; i++) shards[i].open(directory.resolve(String.format("routes-%02d.sqlite", i)));
            LOG.info("PT route cache: {}, RAM payload budget {} MiB, {} disk shards", directory, memoryMb, SHARDS);
        } catch (SQLException | IOException | LinkageError e) { diskFailure(e); }
    }

    long generation() { return generation.get(); }

    byte[] get(PtRouteKey key) {
        if (closed) return null;
        Shard shard = shards[key.hashCode() & (SHARDS - 1)];
        synchronized (shard) {
            byte[] value = shard.hot.get(key);
            if (value != null) { memoryHits.increment(); return value; }
            if (diskDisabled.get()) return null;
            try {
                shard.find.setBytes(1, key.bytes);
                try (ResultSet found = shard.find.executeQuery()) {
                    if (!found.next()) return null;
                    value = found.getBytes(1); shard.remember(key, value); diskHits.increment(); return value;
                }
            } catch (SQLException e) { diskFailure(e); return null; }
        }
    }

    void put(PtRouteKey key, byte[] value) {
        if (closed) return;
        Shard shard = shards[key.hashCode() & (SHARDS - 1)];
        synchronized (shard) {
            shard.remember(key, value);
            if (diskDisabled.get()) return;
            try {
                shard.insert.setBytes(1, key.bytes); shard.insert.setBytes(2, value);
                if (shard.insert.executeUpdate() != 0) bytesWritten.add(key.bytes.length + value.length);
                if (++shard.pending >= 128) { shard.connection.commit(); shard.pending = 0; }
            } catch (SQLException e) { diskFailure(e); }
        }
    }

    /** Table changes start a new key namespace; EventHandler.reset deliberately does not clear iteration reuse. */
    public void invalidate() {
        generation.incrementAndGet();
        for (Shard shard : shards) synchronized (shard) { shard.hot.clear(); shard.hotBytes = 0; }
        LOG.info("PT route cache invalidated after timetable change; generation={}", generation.get());
    }

    @Override public void handleEvent(Event event) { if (event instanceof TransitScheduleChangedEvent) invalidate(); }
    @Override public void reset(int iteration) { }

    public record Statistics(long requests, long memoryHits, long diskHits, long misses,
            long coalesced, long bypasses, long payloadBytesWritten, long generation) { }

    public Statistics statistics() {
        return new Statistics(requests.sum(), memoryHits.sum(), diskHits.sum(), misses.sum(),
                coalesced.sum(), bypasses.sum(), bytesWritten.sum(), generation.get());
    }

    @Override public void notifyIterationEnds(IterationEndsEvent event) {
        flush(); LOG.info("PT route cache iteration {} (cumulative): {}", event.getIteration(), statistics());
    }

    private void flush() {
        if (closed || diskDisabled.get()) return;
        for (Shard shard : shards) synchronized (shard) {
            try { shard.connection.commit(); shard.pending = 0; }
            catch (SQLException e) { diskFailure(e); }
        }
    }

    @Override public void notifyShutdown(ShutdownEvent event) { close(); }

    @Override public void close() {
        if (closed) return;
        closed = true;
        for (Shard shard : shards) synchronized (shard) {
            shard.hot.clear(); shard.hotBytes = 0;
            if (shard.connection == null) continue;
            Connection connection = shard.connection;
            PreparedStatement find = shard.find, insert = shard.insert;
            try (connection; find; insert) {
                connection.commit();
            } catch (SQLException e) { diskFailure(e); }
        }
        LOG.info("PT route cache final: {}", statistics());
    }

    private void diskFailure(Throwable error) {
        if (diskDisabled.compareAndSet(false, true))
            LOG.warn("PT disk cache unavailable; routing continues with the bounded RAM cache", error);
    }

    private final class Shard {
        final LinkedHashMap<PtRouteKey, byte[]> hot = new LinkedHashMap<>(16, 0.75f, true);
        long hotBytes;
        int pending;
        Connection connection;
        PreparedStatement find, insert;

        void open(Path path) throws SQLException, IOException {
            if (Files.exists(path)) throw new IOException("PT cache directory must be fresh: " + path);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            try (Statement sql = connection.createStatement()) {
                sql.execute("PRAGMA journal_mode=WAL");
                sql.execute("PRAGMA synchronous=NORMAL");
                sql.execute("PRAGMA cache_size=-1024");
                sql.execute("CREATE TABLE routes (key BLOB PRIMARY KEY, itinerary BLOB NOT NULL) WITHOUT ROWID");
            }
            connection.setAutoCommit(false);
            find = connection.prepareStatement("SELECT itinerary FROM routes WHERE key=?");
            insert = connection.prepareStatement("INSERT OR IGNORE INTO routes VALUES (?,?)");
        }

        void remember(PtRouteKey key, byte[] value) {
            long size = size(value);
            if (size > hotBytesPerShard) return;
            byte[] previous = hot.put(key, value);
            if (previous != null) hotBytes -= size(previous);
            hotBytes += size;
            var iterator = hot.entrySet().iterator();
            while (hotBytes > hotBytesPerShard && iterator.hasNext()) {
                var entry = iterator.next(); hotBytes -= size(entry.getValue()); iterator.remove();
            }
        }
        // Include digest, key/array/map-entry headers, not only the itinerary payload.
        long size(byte[] value) { return value.length + 160L; }
    }
}
