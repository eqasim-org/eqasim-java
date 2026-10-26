package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import it.unimi.dsi.fastutil.HashCommon;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.controler.events.*;
import org.matsim.core.controler.listener.*;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.pt.router.TransitScheduleChangedEvent;

/** Run-local person-grouped disk store and bounded shared itinerary/template cache. */
public final class PtRouteCache implements BasicEventHandler, IterationEndsListener, ShutdownListener, AutoCloseable {
    private static final Logger LOG = LogManager.getLogger(PtRouteCache.class);
    private static final int SHARDS = 16, PREFETCH_BATCH = 128;
    private static final long PERSON_BYTES = 320;
    private final Shard[] shards = new Shard[SHARDS];
    private final AtomicLong generation = new AtomicLong();
    private final AtomicBoolean diskDisabled = new AtomicBoolean();
    private final long hotBytesPerShard;
    private final ExecutorService prefetchWorkers = Executors.newFixedThreadPool(4, task -> {
        var thread = new Thread(task, "pt-cache-prefetch"); thread.setDaemon(true); return thread;
    });
    final ConcurrentHashMap<PtRouteKey, CompletableFuture<PtCachedItinerary>> inFlight = new ConcurrentHashMap<>();
    final LongAdder requests = new LongAdder(), memoryHits = new LongAdder(), diskHits = new LongAdder();
    final LongAdder misses = new LongAdder(), coalesced = new LongAdder(), bypasses = new LongAdder();
    final LongAdder bytesWritten = new LongAdder(), diskReads = new LongAdder();
    final LongAdder prefetchedPersons = new LongAdder(), prefetchedRoutes = new LongAdder();
    private volatile boolean closed;

    public PtRouteCache(Path directory, int memoryMb) {
        if (memoryMb < 0) throw new IllegalArgumentException("PT cache memoryMb must be nonnegative");
        hotBytesPerShard = (long) memoryMb * 1024 * 1024 / SHARDS;
        for (int i = 0; i < SHARDS; i++) shards[i] = new Shard();
        try {
            Files.createDirectories(directory);
            for (int i = 0; i < SHARDS; i++) shards[i].open(directory.resolve(String.format("routes-%02d.sqlite", i)));
            LOG.info("PT route cache: {}, RAM/template budget {} MiB, {} disk shards", directory, memoryMb, SHARDS);
        } catch (SQLException | IOException | LinkageError e) { diskFailure(e); }
    }

    long generation() { return generation.get(); }
    private static int shardIndex(int person) { return HashCommon.mix(person) & (SHARDS - 1); }

    PtCachedItinerary get(PtRouteKey key) {
        if (closed) return null;
        Shard shard = shards[shardIndex(key.person())];
        synchronized (shard) {
            PersonRoutes person = shard.hot.get(key.person());
            if (person != null && person.generation == key.generation()) {
                var value = person.routes.get(key.departure());
                if (value != null) { memoryHits.increment(); return value; }
                if (person.complete) return null;
            } else person = null;
            if (diskDisabled.get()) return null;
            try {
                if (person == null && hotBytesPerShard >= PERSON_BYTES) {
                    person = shard.loadPerson(key);
                    var value = person.routes.get(key.departure());
                    if (value != null) { diskHits.increment(); return value; }
                    if (person.complete) return null;
                }
                shard.find.setLong(1, key.generation()); shard.find.setInt(2, key.person());
                shard.find.setLong(3, key.departure()); diskReads.increment();
                try (ResultSet found = shard.find.executeQuery()) {
                    if (!found.next()) return null;
                    var value = entry(found.getBytes(1)); shard.remember(key, value);
                    diskHits.increment(); return value;
                }
            } catch (SQLException e) { diskFailure(e); return null; }
        }
    }

    PtCachedItinerary put(PtRouteKey key, byte[] bytes) {
        var value = entry(bytes);
        if (closed) return value;
        Shard shard = shards[shardIndex(key.person())];
        synchronized (shard) {
            shard.remember(key, value);
            if (diskDisabled.get()) return value;
            try {
                shard.insert.setLong(1, key.generation()); shard.insert.setInt(2, key.person());
                shard.insert.setLong(3, key.departure()); shard.insert.setBytes(4, bytes);
                if (shard.insert.executeUpdate() != 0) bytesWritten.add(20L + bytes.length);
                if (++shard.pending >= 128) { shard.connection.commit(); shard.pending = 0; }
            } catch (SQLException e) { diskFailure(e); }
        }
        return value;
    }

    private PtCachedItinerary entry(byte[] bytes) { return new PtCachedItinerary(bytes, hotBytesPerShard > 0); }

    /** Called with exactly the people assigned to DMC, before its worker algorithms start. */
    void prefetch(IntList people) {
        if (closed || diskDisabled.get() || hotBytesPerShard < PERSON_BYTES || people.isEmpty()) return;
        long started = System.nanoTime();
        long epoch = generation();
        var grouped = new IntArrayList[SHARDS];
        for (int i = 0; i < SHARDS; i++) grouped[i] = new IntArrayList();
        for (int person : people) grouped[shardIndex(person)].add(person);
        var tasks = new ArrayList<Future<?>>();
        for (int i = 0; i < SHARDS; i++) {
            Shard shard = shards[i]; IntArrayList selected = grouped[i];
            tasks.add(prefetchWorkers.submit(() -> {
                synchronized (shard) {
                    if (epoch != generation() || closed || diskDisabled.get()) return;
                    try { shard.prefetch(selected, epoch); }
                    catch (SQLException e) { diskFailure(e); }
                }
            }));
        }
        boolean interrupted = false;
        for (var task : tasks) {
            // Do not leave prefetch mutating the cache after routing starts, even on interruption.
            for (;;) try { task.get(); break; }
            catch (InterruptedException e) { interrupted = true; }
            catch (ExecutionException e) { throw new IllegalStateException("PT prefetch failed", e.getCause()); }
        }
        if (interrupted) { Thread.currentThread().interrupt(); throw new IllegalStateException("PT prefetch interrupted"); }
        LOG.info("PT route cache preload: {} selected people in {} ms", people.size(), (System.nanoTime() - started) / 1_000_000.0);
    }

    public void invalidate() {
        generation.incrementAndGet();
        for (Shard shard : shards) synchronized (shard) { shard.hot.clear(); shard.hotBytes = 0; }
        LOG.info("PT route cache invalidated; generation={}", generation.get());
    }

    @Override public void handleEvent(Event event) { if (event instanceof TransitScheduleChangedEvent) invalidate(); }
    @Override public void reset(int iteration) { }

    public record Statistics(long requests, long memoryHits, long diskHits, long misses,
            long coalesced, long bypasses, long payloadBytesWritten, long generation,
            long diskReadQueries, long prefetchedPersons, long prefetchedRoutes, long estimatedMemoryBytes) { }

    public Statistics statistics() {
        long memory = 0;
        for (Shard shard : shards) synchronized (shard) { memory += shard.hotBytes; }
        return new Statistics(requests.sum(), memoryHits.sum(), diskHits.sum(), misses.sum(), coalesced.sum(),
                bypasses.sum(), bytesWritten.sum(), generation.get(), diskReads.sum(),
                prefetchedPersons.sum(), prefetchedRoutes.sum(), memory);
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
        closed = true; prefetchWorkers.shutdown();
        for (Shard shard : shards) synchronized (shard) {
            shard.hot.clear(); shard.hotBytes = 0;
            if (shard.connection == null) continue;
            Connection connection = shard.connection;
            PreparedStatement find = shard.find, insert = shard.insert, person = shard.findPerson, batch = shard.findBatch;
            try (connection; find; insert; person; batch) { connection.commit(); }
            catch (SQLException e) { diskFailure(e); }
        }
        LOG.info("PT route cache final: {}", statistics());
    }

    private void diskFailure(Throwable error) {
        if (diskDisabled.compareAndSet(false, true))
            LOG.warn("PT disk cache unavailable; routing continues with the bounded RAM cache", error);
    }

    private static final class PersonRoutes {
        final long generation;
        final Long2ObjectOpenHashMap<PtCachedItinerary> routes = new Long2ObjectOpenHashMap<>(2);
        boolean complete;
        long bytes = PERSON_BYTES;
        PersonRoutes(long generation) { this.generation = generation; }
        void put(long departure, PtCachedItinerary entry) {
            var previous = routes.put(departure, entry);
            bytes += entry.estimatedBytes() + 32L;
            if (previous != null) bytes -= previous.estimatedBytes() + 32L;
        }
    }

    private final class Shard {
        final LinkedHashMap<Integer, PersonRoutes> hot = new LinkedHashMap<>(16, 0.75f, true);
        long hotBytes;
        int pending;
        Connection connection;
        PreparedStatement find, insert, findPerson, findBatch;

        void open(Path path) throws SQLException, IOException {
            if (Files.exists(path)) throw new IOException("PT cache directory must be fresh: " + path);
            connection = DriverManager.getConnection("jdbc:sqlite:" + path.toAbsolutePath());
            try (Statement sql = connection.createStatement()) {
                sql.execute("PRAGMA journal_mode=WAL"); sql.execute("PRAGMA synchronous=NORMAL");
                sql.execute("PRAGMA cache_size=-1024");
                sql.execute("CREATE TABLE routes (generation INTEGER, person INTEGER, departure INTEGER, itinerary BLOB NOT NULL, PRIMARY KEY(generation,person,departure)) WITHOUT ROWID");
            }
            connection.setAutoCommit(false);
            find = connection.prepareStatement("SELECT itinerary FROM routes WHERE generation=? AND person=? AND departure=?");
            findPerson = connection.prepareStatement("SELECT departure,itinerary FROM routes WHERE generation=? AND person=?");
            findBatch = connection.prepareStatement("SELECT person,departure,itinerary FROM routes WHERE generation=? AND person IN ("
                    + String.join(",", Collections.nCopies(PREFETCH_BATCH, "?")) + ") ORDER BY person,departure");
            insert = connection.prepareStatement("INSERT OR IGNORE INTO routes VALUES (?,?,?,?)");
        }

        PersonRoutes loadPerson(PtRouteKey key) throws SQLException {
            var person = new PersonRoutes(key.generation());
            findPerson.setLong(1, key.generation()); findPerson.setInt(2, key.person()); diskReads.increment();
            try (var rows = findPerson.executeQuery()) {
                person.complete = true;
                while (rows.next()) {
                    var value = entry(rows.getBytes(2));
                    if (person.bytes + value.estimatedBytes() + 32 > hotBytesPerShard) { person.complete = false; break; }
                    person.put(rows.getLong(1), value);
                }
            }
            if (key.generation() == generation()) rememberPerson(key.person(), person);
            return person;
        }

        void remember(PtRouteKey key, PtCachedItinerary value) {
            if (key.generation() != generation() || PERSON_BYTES + value.estimatedBytes() + 32 > hotBytesPerShard) return;
            var person = hot.get(key.person());
            if (person == null) { person = new PersonRoutes(key.generation()); hot.put(key.person(), person); hotBytes += person.bytes; }
            long oldSize = person.bytes; person.put(key.departure(), value); hotBytes += person.bytes - oldSize;
            trim();
        }

        void rememberPerson(int id, PersonRoutes person) {
            if (person.bytes > hotBytesPerShard) return;
            var previous = hot.put(id, person);
            if (previous != null) hotBytes -= previous.bytes;
            hotBytes += person.bytes; trim();
        }

        void trim() {
            var iterator = hot.entrySet().iterator();
            while (hotBytes > hotBytesPerShard && iterator.hasNext()) {
                hotBytes -= iterator.next().getValue().bytes; iterator.remove();
            }
        }

        void prefetch(IntList selected, long epoch) throws SQLException {
            var wanted = new IntOpenHashSet(selected);
            var iterator = hot.entrySet().iterator();
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!wanted.contains(entry.getKey())) { hotBytes -= entry.getValue().bytes; iterator.remove(); }
            }
            var batch = new IntArrayList(PREFETCH_BATCH);
            for (int id : selected) {
                if (hot.containsKey(id)) continue;
                // Reserve bucket overhead as well as payloads. Never evict earlier prefetch work.
                if (hotBytes + PERSON_BYTES > hotBytesPerShard) break;
                var person = new PersonRoutes(epoch); hot.put(id, person); hotBytes += person.bytes;
                batch.add(id);
                if (batch.size() == PREFETCH_BATCH) {
                    if (!prefetchBatch(batch, epoch)) return;
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) prefetchBatch(batch, epoch);
        }

        boolean prefetchBatch(IntList ids, long epoch) throws SQLException {
            findBatch.setLong(1, epoch);
            for (int i = 0; i < PREFETCH_BATCH; i++) findBatch.setInt(i + 2, i < ids.size() ? ids.getInt(i) : -1);
            diskReads.increment();
            try (var rows = findBatch.executeQuery()) {
                while (rows.next()) {
                    var person = hot.get(rows.getInt(1)); var value = entry(rows.getBytes(3));
                    if (hotBytes + value.estimatedBytes() + 32 > hotBytesPerShard) return false;
                    person.put(rows.getLong(2), value); hotBytes += value.estimatedBytes() + 32;
                    prefetchedRoutes.increment();
                }
            }
            for (int id : ids) { hot.get(id).complete = true; prefetchedPersons.increment(); }
            return true;
        }
    }
}
