package org.eqasim.switzerland.ch_cmdp.utils.routing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import static org.junit.Assert.*;

public class TestTripsRouterSession {
    @Test public void httpHealthStaysResponsiveAndRoutingSessionIsReused() throws Exception {
        var road = new org.eqasim.core.scenario.cutter.network.RoadNetwork(network());
        var entered = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        var block = new java.util.concurrent.atomic.AtomicBoolean(true);
        org.matsim.core.router.util.TravelTime time = (link, t, person, vehicle) -> {
            if (block.get()) {
                entered.countDown();
                try { assertTrue(release.await(10, java.util.concurrent.TimeUnit.SECONDS)); }
                catch (InterruptedException e) { throw new RuntimeException(e); }
            }
            return (double) (float) 123.456789;
        };
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var runtime = new TripsRouterServer.RuntimeState(road, time, 2, 1, -1, 0, true, mapper);
        var settings = new TripsRouterServer.Settings("127.0.0.1", 0, 2, 1, 0, 1800, 900,
                -1, 0, true, "unused", "unused");
        var server = TripsRouterServer.buildServer(settings, runtime);
        server.start();
        try (runtime; var client = java.net.http.HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            var trip = new TripsRouter.Trip(); trip.identifier = "one"; trip.destinationX = 2000;
            String body = mapper.writeValueAsString(List.of(trip));
            var request = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/route"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
            var pending = client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            try {
                assertTrue(entered.await(10, java.util.concurrent.TimeUnit.SECONDS));
                var health = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/health"))
                        .timeout(java.time.Duration.ofSeconds(3)).GET().build(),
                        java.net.http.HttpResponse.BodyHandlers.ofString());
                assertEquals(200, health.statusCode()); assertEquals("ok", health.body());
            } finally { block.set(false); release.countDown(); }
            var first = pending.get(10, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(first.body(), 200, first.statusCode());
            assertEquals(2 * (double) (float) 123.456789, mapper.readTree(first.body()).get(0).get("travel_time").asDouble(), 0);
            var field = TripsRouterServer.RuntimeState.class.getDeclaredField("session"); field.setAccessible(true);
            Object session = field.get(runtime);
            var second = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(first.body(), second.body()); assertSame(session, field.get(runtime));
            var update = client.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/config"))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString("{\"routingDistanceUtility\":0.3}"))
                    .build(), java.net.http.HttpResponse.BodyHandlers.ofString());
            assertEquals(200, update.statusCode());
            assertEquals(200, client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString()).statusCode());
            assertNotSame(session, field.get(runtime));
        } finally { release.countDown(); server.stop(0); }
    }

    @Test public void repeatedAndConcurrentRequestsKeepOrderAndUnreachableRows() throws Exception {
        var network = network();
        List<TripsRouter.Trip> trips = new ArrayList<>();
        for (int i = 0; i < 501; i++) {
            var trip = new TripsRouter.Trip(); trip.identifier = "trip" + i;
            trip.originX = i % 2 == 0 ? 0 : 2000;
            trip.destinationX = 2000 - trip.originX; trip.departureTime = 100;
            trips.add(trip);
        }
        var parallelReaders = new java.util.concurrent.CountDownLatch(4);
        org.matsim.core.router.util.TravelTime sharedTimes = (link, time, person, vehicle) -> {
            parallelReaders.countDown();
            try { assertTrue("Short batches did not engage four workers",
                    parallelReaders.await(10, java.util.concurrent.TimeUnit.SECONDS)); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
            return link.getLength() / link.getFreespeed();
        };
        try (var session = new TripsRouter.RoutingSession(network, sharedTimes, 4, 1000, 0);
                var callers = Executors.newFixedThreadPool(2)) {
            var first = callers.submit(() -> session.route(trips, true));
            var second = callers.submit(() -> session.route(trips, true));
            for (var results : List.of(first.get(), second.get())) {
                assertEquals(501, results.size());
                for (int i = 0; i < results.size(); i++) {
                    var result = results.get(i); assertEquals("trip" + i, result.identifier);
                    if (i % 2 == 0) {
                        assertEquals(200, result.travelTime, 0); assertEquals("ab-bc", result.links);
                    } else {
                        assertTrue(Double.isNaN(result.travelTime)); assertTrue(Double.isNaN(result.travelDistance));
                        assertEquals(-1, result.linkCount);
                    }
                }
            }
        }
    }

    private static org.matsim.api.core.v01.network.Network network() {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1000, 0));
        var c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"), new Coord(2000, 0));
        NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 1000, 10, 3600, 1);
        NetworkUtils.createAndAddLink(network, Id.createLinkId("bc"), b, c, 1000, 10, 3600, 1);
        return network;
    }
}
