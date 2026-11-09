package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.*;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.*;
import static org.junit.Assert.*;

public class TestPtRouteCacheTimeWindow {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @Test public void defaultsAndValidation() throws Exception {
        var settings = new PtRouteCacheConfigGroup();
        assertEquals(512, settings.memoryMb); assertEquals(10, settings.departureTimeToleranceSeconds, 0);
        assertThrows(IllegalArgumentException.class, () -> new PtRouteCache(folder.newFolder().toPath(), 1, -1));
        assertThrows(IllegalArgumentException.class, () -> new PtRouteCache(folder.newFolder().toPath(), 1, Double.NaN));
    }

    @Test public void nearestTimesCrossBucketBoundariesAndWorkOnDiskAndInMemory() throws Exception {
        for (int memory : new int[]{0, 1}) {
            var calls = new AtomicInteger();
            try (var cache = new PtRouteCache(folder.newFolder().toPath(), memory)) {
                var router = new CachingPtRoutingModule(request -> {
                    calls.incrementAndGet(); var result = TestPtRouteCache.itinerary(request.getDepartureTime());
                    result.getFirst().getAttributes().putAttribute("source", request.getDepartureTime()); return result;
                }, cache, PopulationUtils.getFactory());
                router.calcRoute(request(100.01));
                var earlier = router.calcRoute(request(99.99));
                assertEquals(1, calls.get());
                assertEquals(99.99, ((Leg) earlier.getFirst()).getDepartureTime().seconds(), 1e-9);
                assertEquals(109.99, ((Activity) earlier.get(1)).getStartTime().seconds(), 1e-9);
                router.calcRoute(request(110.01)); assertEquals(1, calls.get());
                router.calcRoute(request(110.0101)); assertEquals(2, calls.get());
                router.calcRoute(request(140)); assertEquals(3, calls.get());
                var closest = router.calcRoute(request(131)); assertEquals(3, calls.get());
                assertEquals(140.0, (Double) closest.getFirst().getAttributes().getAttribute("source"), 0);
                // Negative times cross zero correctly in both the memory index and SQLite range query.
                router.calcRoute(request(-5)); var negative = router.calcRoute(request(4.9));
                assertEquals(4, calls.get());
                assertEquals(4.9, ((Leg) negative.getFirst()).getDepartureTime().seconds(), 1e-9);
                var one = request(131);
                router.calcRoute(DefaultRoutingRequest.withoutAttributes(one.getFromFacility(), one.getToFacility(), 131,
                        PopulationUtils.getFactory().createPerson(Id.createPersonId("distinct-person"))));
                assertEquals(5, calls.get());
                assertTrue(cache.statistics().timeWindowHits() >= 4);
            }
        }
    }

    @Test public void nearbyHitsDoNotChainBeyondTheOriginalTenSecondWindow() throws Exception {
        for (int memory : new int[]{0, 1}) {
            var calls = new AtomicInteger();
            try (var cache = new PtRouteCache(folder.newFolder().toPath(), memory)) {
                var router = new CachingPtRoutingModule(request -> { calls.incrementAndGet(); return null; },
                        cache, PopulationUtils.getFactory());
                assertNull(router.calcRoute(request(100))); assertNull(router.calcRoute(request(109)));
                assertEquals(1, calls.get());
                assertNull(router.calcRoute(request(118))); assertEquals(2, calls.get());
                assertNull(router.calcRoute(request(128))); assertEquals(2, calls.get());
                cache.invalidate(); assertNull(router.calcRoute(request(128))); assertEquals(3, calls.get());
            }
        }
    }

    @Test public void retimingChangesInitialWaitingAndKeepsScheduledTransfersAndArrival() throws Exception {
        for (int memory : new int[]{0, 1}) {
            var calls = new AtomicInteger();
            try (var cache = new PtRouteCache(folder.newFolder().toPath(), memory)) {
                var router = new CachingPtRoutingModule(request -> { calls.incrementAndGet(); return transit(request.getDepartureTime(), 20); },
                        cache, PopulationUtils.getFactory());
                var original = router.calcRoute(request(100));
                var later = router.calcRoute(request(105));
                assertEquals(1, calls.get());
                assertEquals(105, ((Leg) later.getFirst()).getDepartureTime().seconds(), 0);
                var pt = (Leg) later.get(2); var route = (DefaultTransitPassengerRoute) pt.getRoute();
                assertEquals(115, pt.getDepartureTime().seconds(), 0);
                assertEquals(65, pt.getTravelTime().seconds(), 0); assertEquals(65, route.getTravelTime().seconds(), 0);
                assertEquals(130, route.getBoardingTime().seconds(), 0);
                assertEquals(180, pt.getDepartureTime().seconds() + pt.getTravelTime().seconds(), 0);
                var transfer = (Leg) later.get(4);
                assertEquals(185, transfer.getDepartureTime().seconds(), 0);
                assertEquals(200, ((DefaultTransitPassengerRoute) transfer.getRoute()).getBoardingTime().seconds(), 0);
                assertEquals(230, ((Leg) later.get(6)).getDepartureTime().seconds(), 0);
                var earlier = router.calcRoute(request(95));
                assertEquals(75, ((Leg) earlier.get(2)).getTravelTime().seconds(), 0);
                assertEquals(70, ((Leg) original.get(2)).getTravelTime().seconds(), 0);
                assertNotSame(original.get(2), later.get(2));
                assertArrayEquals(PtItineraryCodec.encode(original), PtItineraryCodec.encode(router.calcRoute(request(100))));
            }
        }
    }

    @Test public void missedBoardingReroutesAndCachesTheNewConnectionWithoutRetryingErrorsTwice() throws Exception {
        for (int memory : new int[]{0, 1}) {
            var calls = new AtomicInteger();
            try (var cache = new PtRouteCache(folder.newFolder().toPath(), memory)) {
                var router = new CachingPtRoutingModule(request -> {
                    calls.incrementAndGet();
                    if (request.getDepartureTime() == 100) return transit(100, 1);
                    if (request.getDepartureTime() == 103) throw new IllegalStateException("routing failure");
                    return transit(request.getDepartureTime(), 20);
                }, cache, PopulationUtils.getFactory());
                router.calcRoute(request(100));
                assertThrows(IllegalStateException.class, () -> router.calcRoute(request(103)));
                assertEquals(2, calls.get()); assertEquals(0, cache.statistics().generation());
                var fresh = router.calcRoute(request(105)); assertEquals(3, calls.get());
                assertEquals(135, ((DefaultTransitPassengerRoute) ((Leg) fresh.get(2)).getRoute()).getBoardingTime().seconds(), 0);
                router.calcRoute(request(105)); assertEquals(3, calls.get());
                assertEquals(2, cache.statistics().infeasibleTimeWindowHits());
            }
        }
    }

    private static RoutingRequest request(double time) { return TestPtRouteCache.request(time); }

    private static List<PlanElement> transit(double departure, double wait) {
        var list = new ArrayList<PlanElement>(TestPtRouteCache.itinerary(departure));
        var first = (Leg) list.getFirst();
        var bus = PopulationUtils.createLeg("pt"); bus.setRoutingMode("pt");
        bus.setDepartureTime(departure + 10); bus.setTravelTime(wait + 50);
        var route = route("bus", departure + 10 + wait, wait + 50); bus.setRoute(route); list.set(2, bus);
        var stage = PopulationUtils.createActivityFromCoord("pt interaction", null); stage.setMaximumDuration(0); list.add(stage);
        var tram = PopulationUtils.createLeg("pt"); tram.setRoutingMode("pt");
        tram.setDepartureTime(departure + 10 + wait + 55); tram.setTravelTime(45);
        tram.setRoute(route("tram", departure + 10 + wait + 70, 45)); list.add(tram);
        var endStage = PopulationUtils.createActivityFromCoord("pt interaction", null); endStage.setMaximumDuration(0); list.add(endStage);
        var end = PopulationUtils.createLeg("walk"); end.setRoutingMode("pt");
        end.setDepartureTime(departure + 10 + wait + 100); end.setTravelTime(10); list.add(end);
        assertEquals(10, first.getTravelTime().seconds(), 0);
        return list;
    }

    private static DefaultTransitPassengerRoute route(String name, double boarding, double travel) {
        var route = new DefaultTransitPassengerRoute(Id.createLinkId("from"), Id.createLinkId("to"),
                Id.create(name + "-from", TransitStopFacility.class), Id.create(name + "-to", TransitStopFacility.class),
                Id.create(name, TransitLine.class), Id.create(name, TransitRoute.class));
        route.setBoardingTime(boarding); route.setTravelTime(travel); return route;
    }
}
