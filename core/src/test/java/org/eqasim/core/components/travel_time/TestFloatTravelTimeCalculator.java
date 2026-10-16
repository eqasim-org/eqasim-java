package org.eqasim.core.components.travel_time;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

public class TestFloatTravelTimeCalculator {
    @Test public void storesFloatAveragesAndReleasesAllCountsBeforeRouting() {
        var network = network();
        var config = settings();
        var collector = new FloatTravelTimeCalculator(network, config);
        var link = network.getLinks().get(Id.createLinkId("ab"));
        for (double duration : new double[]{12.25, 12.75, 13.5}) observe(collector, link, 100, duration);
        assertTrue(collector.getStorageStatistics().observationCountBytes() > 0);
        collector.prepareForRouting();
        var stats = collector.getStorageStatistics();
        assertEquals(0, stats.observationCountBytes());
        assertEquals(121 * 4, stats.valueBytes());
        assertTrue(stats.preparedForRouting());
        assertSame(collector.getLinkTravelTimes(), collector.getLinkTravelTimes());
        assertEquals((double) (float) ((12.25 + 12.75 + 13.5) / 3),
                collector.getLinkTravelTimes().getLinkTravelTime(link, 100, null, null), 0);
        collector.prepareForRouting();
        assertEquals(stats, collector.getStorageStatistics());
    }

    @Test public void congestionSpilloverAndOverflowMatchMatsim() {
        var network = network();
        var settings = settings();
        settings.setMaxTime(3600);
        var collector = new FloatTravelTimeCalculator(network, settings);
        var reference = reference(network, settings);
        var link = network.getLinks().get(Id.createLinkId("ab"));
        for (var observation : List.of(new double[]{100, 3000}, new double[]{1000, 20}, new double[]{100000, 50})) {
            observe(collector, link, observation[0], observation[1]);
            observe(reference, link, observation[0], observation[1]);
        }
        collector.prepareForRouting();
        for (double time : new double[]{-1, 0, 899, 900, 1800, 2700, 3600, 100000}) {
            assertEquals(reference.getLinkTravelTimes().getLinkTravelTime(link, time, null, null),
                    collector.getLinkTravelTimes().getLinkTravelTime(link, time, null, null), 0);
        }
        assertEquals(2100, collector.getLinkTravelTimes().getLinkTravelTime(link, 900, null, null), 0);
    }

    @Test public void interpolationMatchesWithinExpectedPrecision() {
        var network = network();
        var config = settings();
        config.setTravelTimeGetterType("linearinterpolation");
        var collector = new FloatTravelTimeCalculator(network, config);
        var reference = reference(network, config);
        var link = network.getLinks().get(Id.createLinkId("ab"));
        observe(collector, link, 100, 12.3456789); observe(reference, link, 100, 12.3456789);
        observe(collector, link, 1000, 40.1234567); observe(reference, link, 1000, 40.1234567);
        collector.prepareForRouting();
        for (double time : new double[]{-100, 0, 450, 700, 900, 1350, 1700, 108000, 200000}) {
            assertEquals(reference.getLinkTravelTimes().getLinkTravelTime(link, time, null, null),
                    collector.getLinkTravelTimes().getLinkTravelTime(link, time, null, null), 0.0011);
        }
    }

    @Test public void parallelReadsNeverAllocateRowsOrModifyPreparedTables() throws Exception {
        var network = network();
        var collector = new FloatTravelTimeCalculator(network, settings());
        var observed = network.getLinks().get(Id.createLinkId("ab"));
        var unobserved = network.getLinks().get(Id.createLinkId("ba"));
        observe(collector, observed, 100, 12.5);
        collector.prepareForRouting();
        var stats = collector.getStorageStatistics();
        var travelTime = collector.getLinkTravelTimes();
        var pool = Executors.newFixedThreadPool(8);
        try {
            var jobs = new ArrayList<java.util.concurrent.Callable<Double>>();
            for (int worker = 0; worker < 8; worker++) jobs.add(() -> {
                double total = 0;
                for (int i = 0; i < 10000; i++) {
                    total += travelTime.getLinkTravelTime(observed, 100, null, null);
                    total += travelTime.getLinkTravelTime(unobserved, i, null, null);
                }
                return total;
            });
            for (var result : pool.invokeAll(jobs)) assertEquals(225000, result.get(), 0);
        } finally { pool.shutdownNow(); }
        assertEquals(stats, collector.getStorageStatistics());
        unobserved.setFreespeed(20);
        assertEquals(5, travelTime.getLinkTravelTime(unobserved, 100, null, null), 0);
        var type = VehicleUtils.createVehicleType(Id.create("slow", org.matsim.vehicles.VehicleType.class));
        type.setMaximumVelocity(5);
        var vehicle = VehicleUtils.createVehicle(Id.createVehicleId("slow"), type);
        assertEquals(20, travelTime.getLinkTravelTime(observed, 100, null, vehicle), 0);
    }

    @Test public void resetUpdatesTheSameSharedViewWithoutKeepingOldObservations() {
        var network = network();
        var collector = new FloatTravelTimeCalculator(network, settings());
        var link = network.getLinks().get(Id.createLinkId("ab"));
        var view = collector.getLinkTravelTimes();
        observe(collector, link, 100, 20);
        collector.prepareForRouting();
        assertEquals(20, view.getLinkTravelTime(link, 100, null, null), 0);
        collector.reset(1);
        observe(collector, link, 100, 40);
        collector.prepareForRouting();
        assertSame(view, collector.getLinkTravelTimes());
        assertEquals(40, view.getLinkTravelTime(link, 100, null, null), 0);
        assertEquals(0, collector.getStorageStatistics().observationCountBytes());
    }

    @Test public void modeFilteringAndVehicleLifecycleDoNotCountActivitiesOrIgnoredModes() {
        var network = network();
        var config = settings();
        config.setFilterModes(true);
        config.setAnalyzedModes(Set.of("car"));
        var collector = new FloatTravelTimeCalculator(network, config);
        var link = network.getLinks().get(Id.createLinkId("ab"));
        var vehicle = Id.createVehicleId("test");
        var person = Id.createPersonId("test");
        collector.handleEvent(new VehicleEntersTrafficEvent(0, person, link.getId(), vehicle, "pt", 0));
        observe(collector, link, 100, 1000);
        collector.handleEvent(new VehicleLeavesTrafficEvent(1100, person, link.getId(), vehicle, "pt", 1));
        collector.handleEvent(new VehicleEntersTrafficEvent(1200, person, link.getId(), vehicle, "car", 0));
        observe(collector, link, 1200, 12.5);
        collector.handleEvent(new VehicleLeavesTrafficEvent(1212.5, person, link.getId(), vehicle, "car", 1));
        collector.handleEvent(new LinkLeaveEvent(3000, vehicle, link.getId()));
        collector.prepareForRouting();
        assertEquals(10, collector.getLinkTravelTimes().getLinkTravelTime(link, 100, null, null), 0);
        assertEquals(12.5, collector.getLinkTravelTimes().getLinkTravelTime(link, 1200, null, null), 0);
    }

    @Test public void linkToLinkTimesAreSharedPreparedAndResettable() {
        var network = network();
        var config = settings();
        config.setCalculateLinkToLinkTravelTimes(true);
        var collector = new FloatTravelTimeCalculator(network, config);
        var from = network.getLinks().get(Id.createLinkId("ab"));
        var to = network.getLinks().get(Id.createLinkId("ba"));
        var vehicle = Id.createVehicleId("test");
        collector.handleEvent(new LinkEnterEvent(100, vehicle, from.getId()));
        collector.handleEvent(new LinkEnterEvent(125, vehicle, to.getId()));
        collector.prepareForRouting();
        var view = collector.getLinkToLinkTravelTimes();
        assertSame(view, collector.getLinkToLinkTravelTimes());
        assertEquals(25, view.getLinkToLinkTravelTime(from, to, 100, null, null), 0);
        assertEquals(1, collector.getStorageStatistics().turnRows());
        assertEquals(0, collector.getStorageStatistics().observationCountBytes());
        collector.reset(1);
        collector.handleEvent(new LinkEnterEvent(100, vehicle, from.getId()));
        collector.handleEvent(new LinkEnterEvent(145, vehicle, to.getId()));
        collector.prepareForRouting();
        assertEquals(45, view.getLinkToLinkTravelTime(from, to, 100, null, null), 0);
    }

    static TravelTimeCalculatorConfigGroup settings() {
        var config = ConfigUtils.createConfig().travelTimeCalculator();
        config.setTraveltimeBinSize(900);
        config.setMaxTime(108000);
        config.setFilterModes(false);
        return config;
    }

    static TravelTimeCalculator reference(Network network, TravelTimeCalculatorConfigGroup config) {
        var builder = new TravelTimeCalculator.Builder(network);
        builder.setTimeslice(config.getTraveltimeBinSize()); builder.setMaxTime(config.getMaxTime());
        builder.setCalculateLinkTravelTimes(true); builder.configure(config);
        return builder.build();
    }

    static void observe(FloatTravelTimeCalculator collector, Link link, double time, double duration) {
        var vehicle = Id.createVehicleId("test");
        collector.handleEvent(new LinkEnterEvent(time, vehicle, link.getId()));
        collector.handleEvent(new LinkLeaveEvent(time + duration, vehicle, link.getId()));
    }

    private static void observe(TravelTimeCalculator collector, Link link, double time, double duration) {
        var vehicle = Id.createVehicleId("test");
        collector.handleEvent(new LinkEnterEvent(time, vehicle, link.getId()));
        collector.handleEvent(new LinkLeaveEvent(time + duration, vehicle, link.getId()));
    }

    static Network network() {
        var network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = factory.createNode(Id.createNodeId("b"), new Coord(100, 0));
        network.addNode(a); network.addNode(b);
        for (var link : List.of(factory.createLink(Id.createLinkId("ab"), a, b), factory.createLink(Id.createLinkId("ba"), b, a))) {
            link.setLength(100); link.setFreespeed(10); link.setCapacity(1000); link.setNumberOfLanes(1);
            link.setAllowedModes(Set.of("car", "truck", "car_passenger", "bike"));
            network.addLink(link);
        }
        return network;
    }
}
