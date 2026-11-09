package org.eqasim.core.components.traffic.bike;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.eqasim.core.components.routing.speedy.MemoryEfficientSpeedyALTFactory;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

public class TestBikeLinkData {
    @Test public void cachedSlopeAndCostsMatchExistingFormulaAcrossRoadTypesAndGradients() {
        var network = NetworkUtils.createNetwork();
        String[] types = {"primary", "secondary", "motorway", "cycleway", "unknown"};
        double[] grades = {-12, 0, 2.1, 5.1, 10.1, 20};
        for (String type : types) for (double grade : grades) {
            var link = link(network, type + grade, grade);
            link.getAttributes().putAttribute("osm:way:highway", type);
        }
        var data = new BikeLinkData(network);
        for (Link link : network.getLinks().values()) {
            assertEquals(BikeGradientBasedLinkSpeedCalculator.slopeSpeed(link), data.slopeSpeed(link), 5e-7);
            for (double time : new double[]{0, 10, 1000}) {
                double expected = time + BikeDisutilities.linkTypeDisutility(link, time)
                        + BikeDisutilities.gradientDisutility(link, time)
                        + BikeDisutilities.distanceDisutility(link, time);
                assertEquals(expected, data.disutility(link, time), 2e-4);
            }
        }
        // An independently prepared small network exercises sparse global link IDs.
        var sparse = NetworkUtils.createNetwork();
        var other = link(sparse, "sparse-last", 6);
        var sparseData = new BikeLinkData(sparse);
        assertEquals(BikeGradientBasedLinkSpeedCalculator.slopeSpeed(other), sparseData.slopeSpeed(other), 5e-7);
        assertEquals(0, new BikeLinkData(NetworkUtils.createNetwork()).disutility(other, 0)
                - BikeDisutilities.distanceDisutility(other, 0), 0);
    }

    @Test public void staticSnapshotDoesNotFreezeDelaysOrSpeedLimits() {
        var network = NetworkUtils.createNetwork();
        var link = link(network, "dynamic", 0);
        var data = new BikeLinkData(network);
        var calculator = new BikeGradientBasedLinkSpeedCalculator((l, t, id) -> t / 10, data);
        var time = new BikeTravelTime(calculator);
        double base = link.getLength() / data.slopeSpeed(link);
        assertEquals(base + 5, time.getLinkTravelTime(link, 100, null, null), 1e-9);
        assertEquals(base + 10, time.getLinkTravelTime(link, 200, null, null), 1e-9);
        link.setFreespeed(2.5);
        assertEquals(40, time.getLinkTravelTime(link, 200, null, null), 0);
        link.setFreespeed(100);
        var type = VehicleUtils.createVehicleType(Id.createVehicleTypeId("limited"));
        type.setMaximumVelocity(4);
        var vehicle = VehicleUtils.createVehicle(Id.createVehicleId("limited"), type);
        assertEquals(35, time.getLinkTravelTime(link, 200, null, vehicle), 1e-9);
        // Geometry/road type are a snapshot; no attribute or slope work on lookup.
        link.getToNode().setCoord(new Coord(100, 0, 150));
        link.getAttributes().putAttribute("osm:way:highway", "motorway");
        assertEquals((float) 5.9, data.slopeSpeed(link), 0);
        assertEquals(30, data.disutility(link, 10), 1e-6);
    }

    @Test public void floatCostsKeepTheSameGaussianDrawSequenceAndFastPathDoesNotQueryTimeAgain() {
        var network = NetworkUtils.createNetwork();
        var link = link(network, "noise", 6);
        link.getAttributes().putAttribute("osm:way:highway", "primary");
        var calls = new AtomicInteger();
        TravelTime time = (l, t, person, vehicle) -> { calls.incrementAndGet(); return 20 + t; };
        MatsimRandom.reset(31);
        var original = new BikeTravelDisutility(time);
        double[] expected = new double[100];
        for (int i = 0; i < expected.length; i++) expected[i] = original.getLinkTravelDisutility(link, i, null, null);
        assertEquals(100, calls.get());
        MatsimRandom.reset(31);
        var cached = new BikeTravelDisutility(time, new BikeLinkData(network));
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], cached.getLinkTravelDisutility(link, i, null, null, 20 + i), 2e-5);
        }
        assertEquals(100, calls.get());
    }

    @Test public void speedyAltReusesTimeOnlyForTheSameCalculator() {
        var network = NetworkUtils.createNetwork();
        var link = link(network, "one-edge", 0);
        var calls = new AtomicInteger();
        TravelTime time = (l, t, person, vehicle) -> { calls.incrementAndGet(); return 20; };
        var cost = new BikeTravelDisutility(time, new BikeLinkData(network));
        var factory = new MemoryEfficientSpeedyALTFactory();
        var router = factory.createPathCalculator(network, cost, time);
        assertNotNull(router.calcLeastCostPath(link.getFromNode(), link.getToNode(), 0, null, null));
        assertEquals(1, calls.get());
        calls.set(0);
        TravelTime other = (l, t, person, vehicle) -> 30;
        router = factory.createPathCalculator(network, cost, other);
        assertEquals(30, router.calcLeastCostPath(link.getFromNode(), link.getToNode(), 0, null, null).travelTime, 0);
        assertEquals(1, calls.get()); // Cost retains its own calculator; no incorrect reuse of 30.
    }

    private static Link link(Network network, String id, double grade) {
        var f = network.getFactory();
        var from = f.createNode(Id.createNodeId(id + "from"), new Coord(0, 0, 100));
        var to = f.createNode(Id.createNodeId(id + "to"), new Coord(100, 0, 100 + grade));
        network.addNode(from); network.addNode(to);
        var link = f.createLink(Id.createLinkId(id), from, to);
        link.setLength(100); link.setFreespeed(100); link.setAllowedModes(Set.of("bike"));
        network.addLink(link);
        return link;
    }
}
