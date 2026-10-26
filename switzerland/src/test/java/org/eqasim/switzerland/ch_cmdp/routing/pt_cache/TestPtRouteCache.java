package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.*;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.pt.router.TransitScheduleChangedEvent;

import static org.junit.Assert.*;

public class TestPtRouteCache {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void diskHitsPreserveItinerariesAndReturnIndependentObjects() throws Exception {
        var count = new AtomicInteger();
        RoutingModule delegate = request -> { count.incrementAndGet(); return itinerary(request.getDepartureTime()); };
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 0)) {
            var router = new CachingPtRoutingModule(delegate, cache, PopulationUtils.getFactory());
            var request = request(100);
            var first = router.calcRoute(request);
            var second = router.calcRoute(request);
            assertEquals(1, count.get()); assertEquals(1, cache.statistics().diskHits());
            assertArrayEquals(PtItineraryCodec.encode(first), PtItineraryCodec.encode(second));
            assertNotSame(first, second); assertNotSame(first.getFirst(), second.getFirst());
            var leg = (Leg) first.getFirst();
            leg.getRoute().setDistance(9999); leg.getAttributes().putAttribute("mutable", "changed");
            var third = (Leg) router.calcRoute(request).getFirst();
            assertEquals(100, third.getRoute().getDistance(), 0);
            assertNull(third.getAttributes().getAttribute("mutable"));
            assertEquals(1, count.get());
            cache.reset(2); router.calcRoute(request); assertEquals(1, count.get());
            cache.handleEvent(new TransitScheduleChangedEvent(200));
            router.calcRoute(request); assertEquals(2, count.get());
        }
    }

    @Test public void keysUseOnlyPersonAndExactDepartureAndIgnoreAllMetadata() throws Exception {
        var calls = new AtomicInteger();
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            var router = new CachingPtRoutingModule(request -> { calls.incrementAndGet(); return itinerary(100); },
                    cache, PopulationUtils.getFactory());
            var one = request(100.1); var two = request(100.9);
            router.calcRoute(one); router.calcRoute(one); router.calcRoute(two);
            assertEquals(2, calls.get());
            one.getPerson().getAttributes().putAttribute("carConstant", 2.0);
            one.getPerson().getAttributes().putAttribute("opaque", new Object());
            one.getAttributes().putAttribute("opaque", new Object());
            router.calcRoute(one); assertEquals(2, calls.get());
            var otherPerson = DefaultRoutingRequest.withoutAttributes(one.getFromFacility(), one.getToFacility(),
                    one.getDepartureTime(), PopulationUtils.getFactory().createPerson(Id.createPersonId("other")));
            router.calcRoute(otherPerson); assertEquals(3, calls.get());
            var otherLocations = DefaultRoutingRequest.withoutAttributes(one.getToFacility(), one.getFromFacility(),
                    one.getDepartureTime(), one.getPerson());
            router.calcRoute(otherLocations); assertEquals(3, calls.get());
            assertEquals(0, cache.statistics().bypasses());
            var anonymous = DefaultRoutingRequest.withoutAttributes(one.getFromFacility(), one.getToFacility(), 100, null);
            router.calcRoute(anonymous); router.calcRoute(anonymous);
            assertEquals(5, calls.get()); assertEquals(2, cache.statistics().bypasses());
        }
    }

    @Test public void parallelWorkersCoalesceSameRequestAndDoNotShareMutableRoutes() throws Exception {
        var calls = new AtomicInteger();
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1);
        RoutingModule delegate = request -> {
            calls.incrementAndGet(); entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
            catch (InterruptedException e) { throw new AssertionError(e); }
            return itinerary(100);
        };
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1);
                var workers = Executors.newFixedThreadPool(8)) {
            var request = request(100);
            var router = new CachingPtRoutingModule(delegate, cache, PopulationUtils.getFactory());
            var futures = new ArrayList<Future<List<? extends PlanElement>>>();
            futures.add(workers.submit(() -> router.calcRoute(request)));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            for (int i = 0; i < 7; i++) futures.add(workers.submit(() -> router.calcRoute(request)));
            release.countDown();
            var identities = Collections.newSetFromMap(new IdentityHashMap<PlanElement, Boolean>());
            for (var future : futures) identities.add(future.get(10, TimeUnit.SECONDS).getFirst());
            assertEquals(8, identities.size()); assertEquals(1, calls.get());
            assertEquals(8, cache.statistics().requests());
        } finally { release.countDown(); }
    }

    @Test public void noRouteIsCachedButExceptionsAreRetried() throws Exception {
        var count = new AtomicInteger();
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 0)) {
            var noRoute = new CachingPtRoutingModule(request -> { count.incrementAndGet(); return null; },
                    cache, PopulationUtils.getFactory());
            assertNull(noRoute.calcRoute(request(100))); assertNull(noRoute.calcRoute(request(100)));
            assertEquals(1, count.get());
            var failing = new CachingPtRoutingModule(request -> { count.incrementAndGet(); throw new IllegalStateException("failure"); },
                    cache, PopulationUtils.getFactory());
            assertThrows(IllegalStateException.class, () -> failing.calcRoute(request(200)));
            assertThrows(IllegalStateException.class, () -> failing.calcRoute(request(200)));
            assertEquals(3, count.get()); assertTrue(cache.inFlight.isEmpty());
        }
    }

    @Test public void switchDefaultsOffAndRejectsUnsupportedDynamicRouting() throws Exception {
        var config = ConfigUtils.createConfig(new SwissRailRaptorConfigGroup());
        PtRouteCacheModule.applyCommand(command(), config);
        var settings = ConfigUtils.addOrGetModule(config, PtRouteCacheConfigGroup.class);
        assertFalse(settings.enabled);
        PtRouteCacheModule.applyCommand(command("--cache-pt-routes", "true"), config);
        assertTrue(settings.enabled);
        PtRouteCacheModule.applyCommand(command("--cache-pt-routes", "false"), config);
        assertFalse(settings.enabled);
        assertThrows(CommandLine.ConfigurationException.class,
                () -> PtRouteCacheModule.applyCommand(command("--cache-pt-routes", "maybe"), config));
        PtRouteCacheModule.validate(config, settings);
        ConfigUtils.addOrGetModule(config, SwissRailRaptorConfigGroup.class).setUseCapacityConstraints(true);
        assertThrows(IllegalArgumentException.class, () -> PtRouteCacheModule.validate(config, settings));
        assertNotEquals(PtRouteCacheModule.cacheDirectory(config, settings), PtRouteCacheModule.cacheDirectory(config, settings));
    }

    @Test public void corruptRecordAndUnavailableDiskFallBackToRouting() throws Exception {
        var request = request(100); var calls = new AtomicInteger();
        RoutingModule delegate = ignored -> { calls.incrementAndGet(); return itinerary(100); };
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            cache.put(PtRouteKey.of(request, cache.generation()), new byte[]{1, 2});
            var router = new CachingPtRoutingModule(delegate, cache, PopulationUtils.getFactory());
            assertNotNull(router.calcRoute(request)); assertEquals(1, calls.get());
            assertEquals(1, cache.statistics().generation());
        }
        // A regular file cannot serve as the cache directory. The hot cache can still be used.
        try (var cache = new PtRouteCache(folder.newFile().toPath(), 1)) {
            var router = new CachingPtRoutingModule(delegate, cache, PopulationUtils.getFactory());
            router.calcRoute(request); router.calcRoute(request);
            assertEquals(2, calls.get()); assertEquals(1, cache.statistics().memoryHits());
        }
    }

    @Test public void xmlConfigurationAndRelativeDirectoryRoundTrip() throws Exception {
        var config = ConfigUtils.createConfig(new PtRouteCacheConfigGroup());
        var settings = ConfigUtils.addOrGetModule(config, PtRouteCacheConfigGroup.class);
        settings.enabled = true; settings.memoryMb = 12; settings.directory = "relative-cache";
        var file = folder.newFile("config.xml");
        new ConfigWriter(config).write(file.toString());
        var restored = ConfigUtils.loadConfig(file.toString(), new PtRouteCacheConfigGroup());
        var values = ConfigUtils.addOrGetModule(restored, PtRouteCacheConfigGroup.class);
        assertTrue(values.enabled); assertEquals(12, values.memoryMb);
        assertTrue(PtRouteCacheModule.cacheDirectory(restored, values).startsWith(folder.getRoot().toPath().resolve("relative-cache")));
    }

    @Test public void personBatchLoadingAvoidsOneDiskReadPerDeparture() throws Exception {
        var first = request(100);
        var key = PtRouteKey.of(first, 0);
        var calls = new AtomicInteger();
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            for (int i = 0; i < 8; i++) cache.put(new PtRouteKey(key.person(), Double.doubleToLongBits(100 + i), 0),
                    PtItineraryCodec.encode(itinerary(100 + i)));
            // Preparing an unrelated selection evicts the original person's RAM records, not their disk data.
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(Id.createPersonId("unselected").index()));
            long reads = cache.statistics().diskReadQueries();
            var router = new CachingPtRoutingModule(ignored -> { calls.incrementAndGet(); return null; }, cache, PopulationUtils.getFactory());
            for (int i = 0; i < 8; i++) assertNotNull(router.calcRoute(request(100 + i)));
            assertEquals(0, calls.get()); assertEquals(reads + 1, cache.statistics().diskReadQueries());
            assertEquals(7, cache.statistics().memoryHits());
        }
    }

    @Test public void selectedPeopleArePrefetchedBeforeDelegateWorkersAndTemplatesRemainPrivate() throws Exception {
        var request = request(100); var person = request.getPerson();
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            cache.put(PtRouteKey.of(request, 0), PtItineraryCodec.encode(itinerary(100)));
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(Id.createPersonId("other-selection").index()));
            var runs = new AtomicInteger(); var finishes = new AtomicInteger();
            var original = new org.matsim.core.replanning.PlanStrategy() {
                public void init(org.matsim.core.replanning.ReplanningContext context) { }
                public void run(HasPlansAndId<Plan, Person> selected) { assertSame(person, selected); runs.incrementAndGet(); }
                public void finish() {
                    finishes.incrementAndGet();
                    assertEquals(1, cache.statistics().prefetchedRoutes());
                    long reads = cache.statistics().diskReadQueries();
                    var router = new CachingPtRoutingModule(ignored -> { fail("Prefetched route should avoid routing"); return null; },
                            cache, PopulationUtils.getFactory());
                    var first = router.calcRoute(request); var second = router.calcRoute(request);
                    try { assertArrayEquals(PtItineraryCodec.encode(first), PtItineraryCodec.encode(second)); }
                    catch (java.io.IOException e) { throw new AssertionError(e); }
                    assertNotSame(first.getFirst(), second.getFirst());
                    assertNotSame(((Activity) first.get(1)).getCoord(), ((Activity) second.get(1)).getCoord());
                    ((Activity) first.get(1)).setType("mutated");
                    ((Leg) first.getFirst()).getRoute().setRouteDescription("mutated");
                    var third = router.calcRoute(request);
                    assertEquals("pt interaction", ((Activity) third.get(1)).getType());
                    assertEquals("test description", ((Leg) third.getFirst()).getRoute().getRouteDescription());
                    assertEquals(reads, cache.statistics().diskReadQueries());
                }
                public String toString() { return "DiscreteModeChoice-original"; }
            };
            var strategy = new PtCachePrefetchingStrategy(original, cache);
            assertEquals(original.toString(), strategy.toString());
            strategy.init(() -> 1); strategy.run(person);
            assertEquals(1, runs.get()); assertEquals(0, finishes.get());
            strategy.finish(); assertEquals(1, finishes.get());
        }
    }

    @Test public void prefetchBatchesDatabaseQueriesAndRespectsMemoryBudget() throws Exception {
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            var selected = new it.unimi.dsi.fastutil.ints.IntArrayList();
            byte[] bytes = PtItineraryCodec.encode(itinerary(100));
            for (int i = 0; i < 400; i++) {
                int person = Id.createPersonId("batch-" + i).index(); selected.add(person);
                cache.put(new PtRouteKey(person, Double.doubleToLongBits(100), 0), bytes);
            }
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(Id.createPersonId("clear-hot").index()));
            long reads = cache.statistics().diskReadQueries(); cache.prefetch(selected);
            assertTrue(cache.statistics().prefetchedRoutes() > 0);
            assertTrue(cache.statistics().diskReadQueries() - reads <= 16);
            assertTrue(cache.statistics().estimatedMemoryBytes() <= 1024 * 1024);
            // Oversized records stay on disk and still route correctly with partial person buckets.
            var large = itinerary(200); ((Leg) large.getFirst()).getRoute().setRouteDescription("x".repeat(100_000));
            var key = PtRouteKey.of(request(200), 0);
            cache.put(key, PtItineraryCodec.encode(large));
            assertArrayEquals(PtItineraryCodec.encode(large), PtItineraryCodec.encode(cache.get(key).restore(PopulationUtils.getFactory())));
            assertTrue(cache.statistics().estimatedMemoryBytes() <= 1024 * 1024);
        }
    }

    @Test public void nullAndEmptyRoutesArePrefetchedWithoutRepeatingFailedSearches() throws Exception {
        var calls = new AtomicInteger();
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 1)) {
            var router = new CachingPtRoutingModule(request -> {
                calls.incrementAndGet(); return request.getDepartureTime() == 100 ? null : List.of();
            }, cache, PopulationUtils.getFactory());
            assertNull(router.calcRoute(request(100))); assertTrue(router.calcRoute(request(200)).isEmpty());
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(Id.createPersonId("clear-negative").index()));
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(request(100).getPerson().getId().index()));
            assertNull(router.calcRoute(request(100))); assertTrue(router.calcRoute(request(200)).isEmpty());
            assertEquals(2, calls.get()); assertEquals(2, cache.statistics().prefetchedRoutes());
            cache.invalidate(); assertNull(router.calcRoute(request(100))); assertEquals(3, calls.get());
        }
    }

    @Test public void prefetchContinuesAcrossFullBatchesInTheSameShard() throws Exception {
        try (var cache = new PtRouteCache(folder.newFolder().toPath(), 8)) {
            var selected = new it.unimi.dsi.fastutil.ints.IntArrayList();
            for (int i = 0; selected.size() < 257; i++) {
                int person = Id.createPersonId("same-shard-" + i).index();
                if ((it.unimi.dsi.fastutil.HashCommon.mix(person) & 15) != 0) continue;
                selected.add(person);
                cache.put(new PtRouteKey(person, Double.doubleToLongBits(100), 0), PtItineraryCodec.encode(null));
            }
            cache.prefetch(it.unimi.dsi.fastutil.ints.IntArrayList.of(Id.createPersonId("clear-batches").index()));
            long before = cache.statistics().diskReadQueries();
            cache.prefetch(selected);
            assertEquals(before + 3, cache.statistics().diskReadQueries());
            assertEquals(257, cache.statistics().prefetchedRoutes());
            for (int person : selected)
                assertNull(cache.get(new PtRouteKey(person, Double.doubleToLongBits(100), 0)).restore(PopulationUtils.getFactory()));
            assertEquals(before + 3, cache.statistics().diskReadQueries());
        }
    }

    private static CommandLine command(String... args) throws Exception {
        return new CommandLine.Builder(args).allowOptions(PtRouteCacheModule.OPTION).build();
    }

    static RoutingRequest request(double departure) {
        var network = NetworkUtils.createNetwork();
        var from = NetworkUtils.createAndAddNode(network, Id.createNodeId("from"), new Coord(0, 0));
        var to = NetworkUtils.createAndAddNode(network, Id.createNodeId("to"), new Coord(1000, 0));
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("link"), from, to, 1000, 10, 1000, 1);
        return DefaultRoutingRequest.withoutAttributes(FacilitiesUtils.wrapLinkAndCoord(link, from.getCoord()),
                FacilitiesUtils.wrapLinkAndCoord(link, to.getCoord()), departure,
                PopulationUtils.getFactory().createPerson(Id.createPersonId("person")));
    }

    static List<PlanElement> itinerary(double departure) {
        var f = PopulationUtils.getFactory();
        var leg = f.createLeg("walk"); leg.setRoutingMode("pt"); leg.setDepartureTime(departure); leg.setTravelTime(10);
        var route = RouteUtils.createGenericRouteImpl(Id.createLinkId("from"), Id.createLinkId("to"));
        route.setRouteDescription("test description"); route.setDistance(100); route.setTravelTime(10); leg.setRoute(route);
        leg.getAttributes().putAttribute("test-int", 3); leg.getAttributes().putAttribute("test-float", 4.5F);
        var stage = f.createActivityFromCoord("pt interaction", new Coord(1, 2, 3));
        stage.setLinkId(Id.createLinkId("to")); stage.setMaximumDuration(0); stage.setStartTime(departure + 10);
        var last = f.createLeg("walk"); last.setRoutingMode("pt"); last.setTravelTime(20);
        return List.of(leg, stage, last);
    }
}
