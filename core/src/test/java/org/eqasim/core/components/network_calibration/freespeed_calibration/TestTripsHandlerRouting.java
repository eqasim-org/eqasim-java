package org.eqasim.core.components.network_calibration.freespeed_calibration;

import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.eqasim.core.components.travel_time.FloatTravelTimeCalculator;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import static org.junit.Assert.*;

public class TestTripsHandlerRouting {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void successiveOptimizedPassesUseUpdatedFreespeedsAndSharedFloatFallback() throws Exception {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1000, 0));
        var c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"), new Coord(2000, 0));
        var ab = NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 1000, 10, 3600, 1);
        var bc = NetworkUtils.createAndAddLink(network, Id.createLinkId("bc"), b, c, 1000, 10, 3600, 1);
        for (var link : List.of(ab, bc)) {
            link.getAttributes().putAttribute("osm:way:highway", "primary");
            link.getAttributes().putAttribute("municipalityType", "urban");
        }
        // No observations: the shared collector must use the currently calibrated
        // free speed, rather than the speed from the original filtered network.
        var collector = new FloatTravelTimeCalculator(network, ConfigUtils.createConfig().travelTimeCalculator());
        collector.prepareForRouting();
        var config = new NetworkCalibrationConfigGroup(); config.setActivate(true); config.setCalibrate(true);
        var settings = config.getFreeSpeedCalibrationConfigGroup(); config.setObjective("freespeed");
        settings.setMinTripDistance(1); settings.setMinTripTravelTime(1);
        var file = temporary.newFile("observed.csv").toPath();
        Files.writeString(file, "identifier,departure_x,departure_y,arrival_x,arrival_y,departure_time,travel_time,traveled_distance\n"
                + "1,0,0,2000,0,100,400,2000\n2,0,0,2000,0,100,400,2000\n");
        settings.setObservedTripsFile(file.toString());
        var simulationFactory = new MemoryEfficientSpeedyALTFactory();
        var handler = new TripsHandler(network, config, settings, new LinkCategorizer(network, config),
                collector.getLinkTravelTimes(), () -> simulationFactory, 2);
        assertEquals(400, handler.routeTrips().values().iterator().next().simulatedTime, 1e-7);
        ab.setFreespeed(5); bc.setFreespeed(5);
        assertEquals(800, handler.routeTrips().values().iterator().next().simulatedTime, 1e-7);
        assertTrue("Calibration passes must not retain obsolete landmark sets in the simulation factory",
                simulationFactory.getStatistics().isEmpty());
        assertEquals(0, collector.getStorageStatistics().linkRows());
        assertEquals(0, collector.getStorageStatistics().observationCountBytes());
    }

    @Test public void injectedFactoryAndSharedFloatTimesUseOnlyConfiguredPrivateWorkers() throws Exception {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(1000, 0));
        var c = NetworkUtils.createAndAddNode(network, Id.createNodeId("c"), new Coord(2000, 0));
        var ab = NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), a, b, 1000, 10, 3600, 1);
        var bc = NetworkUtils.createAndAddLink(network, Id.createLinkId("bc"), b, c, 1000, 10, 3600, 1);
        var collector = new FloatTravelTimeCalculator(network, ConfigUtils.createConfig().travelTimeCalculator());
        for (var link : List.of(ab, bc)) {
            link.getAttributes().putAttribute("osm:way:highway", "primary");
            link.getAttributes().putAttribute("municipalityType", "urban");
            collector.handleEvent(new LinkEnterEvent(100, Id.createVehicleId("car"), link.getId()));
            collector.handleEvent(new LinkLeaveEvent(300, Id.createVehicleId("car"), link.getId()));
        }
        collector.prepareForRouting();
        var time = collector.getLinkTravelTimes();
        var created = new AtomicInteger(); var resolved = new AtomicInteger();
        var calls = new AtomicInteger(); var concurrent = new CountDownLatch(2);
        var optimized = new MemoryEfficientSpeedyALTFactory();
        LeastCostPathCalculatorFactory tracked = (net, cost, actualTime) -> {
            assertSame(time, actualTime); created.incrementAndGet();
            var router = optimized.createPathCalculator(net, cost, actualTime);
            var inUse = new AtomicBoolean();
            return (from, to, departure, person, vehicle) -> {
                assertTrue("A mutable router was shared concurrently", inUse.compareAndSet(false, true));
                try {
                    calls.incrementAndGet(); concurrent.countDown();
                    try { assertTrue(concurrent.await(10, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new RuntimeException(e); }
                    return router.calcLeastCostPath(from, to, departure, person, vehicle);
                } finally { inUse.set(false); }
            };
        };
        var config = new NetworkCalibrationConfigGroup(); config.setActivate(true); config.setCalibrate(true);
        var settings = config.getFreeSpeedCalibrationConfigGroup(); config.setObjective("freespeed");
        settings.setMinTripDistance(1); settings.setMinTripTravelTime(1);
        var file = Files.createTempFile("calibration-observed", ".csv");
        try {
            var csv = new StringBuilder("identifier,departure_x,departure_y,arrival_x,arrival_y,departure_time,travel_time,traveled_distance\n");
            for (int i = 0; i < 101; i++) csv.append(i).append(",0,0,2000,0,100,400,2000\n");
            Files.writeString(file, csv); settings.setObservedTripsFile(file.toString());
            var handler = new TripsHandler(network, config, settings, new LinkCategorizer(network, config), time,
                    () -> { resolved.incrementAndGet(); return tracked; }, 2);
            var stats = handler.routeTrips();
            assertEquals(1, resolved.get()); assertEquals(2, created.get()); assertEquals(101, calls.get());
            assertEquals(1, stats.size());
            var group = stats.values().iterator().next();
            assertEquals(202, group.tripCount);
            assertEquals(40400, group.simulatedTime, 1e-7);
            assertEquals(40400, group.observedTime, 1e-7);
        } finally { Files.deleteIfExists(file); }
    }
}
