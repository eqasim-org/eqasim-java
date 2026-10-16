package org.eqasim.core.simulation.vdf.travel_time;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.travel_time.function.VolumeDelayFunction;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestVDFTravelTime {
    @Test
    public void routingViewUsesReadOnlyDelayWhileSimulationRetainsVehicleHistory() {
        var network = NetworkUtils.createNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("routing-a"), new Coord(0, 0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("routing-b"), new Coord(100, 0));
        var link = network.getFactory().createLink(Id.createLinkId("routing-link"), a, b);
        link.setLength(100); link.setFreespeed(10); network.addLink(link);
        var historyCalls = new java.util.concurrent.atomic.AtomicInteger();
        CrossingPenalty delay = new CrossingPenalty() {
            @Override public double calculateCrossingPenalty(Link input, double time, Id<org.matsim.vehicles.Vehicle> vehicle) {
                historyCalls.incrementAndGet(); return 3;
            }
            @Override public double estimateCrossingPenalty(Link input, double time) { return 4; }
        };
        var times = new VDFTravelTime(new VDFScope(0, 900, 900), 1, 1, 1, network,
                (time, flow, capacity, input) -> 10, delay);
        org.junit.Assert.assertSame(times.getRoutingTravelTimes(), times.getRoutingTravelTimes());
        assertEquals(14, times.getRoutingTravelTimes().getLinkTravelTime(link, 100, null, null), 0);
        assertEquals(0, historyCalls.get());
        assertEquals(13, times.getLinkTravelTime(link, 100, null, null), 0);
        assertEquals(1, historyCalls.get());
    }

    @Test
    public void floatTablesUpdateAndKeepTheLegacyDoubleFileFormat() throws Exception {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var a = factory.createNode(Id.createNodeId("float-a"), new Coord(0, 0));
        var b = factory.createNode(Id.createNodeId("float-b"), new Coord(100, 0));
        network.addNode(a); network.addNode(b);
        var link = factory.createLink(Id.createLinkId("float-link"), a, b);
        link.setLength(100); link.setFreespeed(9); link.setCapacity(1000); network.addLink(link);
        var scope = new VDFScope(0, 1800, 900);
        var times = new VDFTravelTime(scope, 1, 1, 1, network,
                (time, flow, capacity, input) -> 12.3456789 + flow / 100, (input, time, vehicle) -> 0);
        assertEquals((double) (float) (100.0 / 9), times.getLinkTravelTime(link, 0, null, null), 0);
        var counts = new org.matsim.api.core.v01.IdMap<Link, double[]>(Link.class);
        counts.put(link.getId(), new double[]{1000, 2000});
        times.update(counts);
        assertEquals((double) (float) 22.3456789, times.getLinkTravelTime(link, 0, null, null), 0);
        assertEquals((double) (float) 32.3456789, times.getLinkTravelTime(link, 900, null, null), 0);
        var field = VDFTravelTime.class.getDeclaredField("travelTimes"); field.setAccessible(true);
        assertTrue(((java.util.Map<?, ?>) field.get(times)).get(link.getId()) instanceof float[]);
        var file = java.nio.file.Files.createTempFile("float-vdf", ".bin");
        try {
            // An existing double-encoded input remains readable.
            try (var output = new java.io.DataOutputStream(java.nio.file.Files.newOutputStream(file))) {
                output.writeInt(1); output.writeInt(2); output.writeUTF(link.getId().toString());
                output.writeDouble(14.123456789); output.writeDouble(28.987654321);
            }
            times.readFrom(file.toUri().toURL());
            assertEquals((double) (float) 14.123456789, times.getLinkTravelTime(link, 0, null, null), 0);
            times.write(file.toFile());
            try (var input = new java.io.DataInputStream(java.nio.file.Files.newInputStream(file))) {
                assertEquals(1, input.readInt()); assertEquals(2, input.readInt());
                assertEquals(link.getId().toString(), input.readUTF());
                assertEquals((double) (float) 14.123456789, input.readDouble(), 0);
                assertEquals((double) (float) 28.987654321, input.readDouble(), 0);
                assertEquals(-1, input.read());
            }
        } finally { java.nio.file.Files.deleteIfExists(file); }
    }

    @Test
    public void addsCrossingPenaltyOnlyWhenTravelTimeIsRequested() {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var fromNode = factory.createNode(Id.createNodeId("from"), new Coord(0.0, 0.0));
        var toNode = factory.createNode(Id.createNodeId("to"), new Coord(100.0, 0.0));
        network.addNode(fromNode);
        network.addNode(toNode);

        Link link = factory.createLink(Id.createLinkId("link"), fromNode, toNode);
        link.setLength(100.0);
        link.setFreespeed(10.0);
        network.addLink(link);

        VDFScope scope = new VDFScope(0.0, 3600.0, 3600.0);
        VolumeDelayFunction vdf = (time, flow, capacity, inputLink) -> 0.0;
        CrossingPenalty crossingPenalty = (inputLink, time, vehicleId) -> 3.0;
        VDFTravelTime travelTime = new VDFTravelTime(
                scope, 1.0, 1.0, 1.0, network, vdf, crossingPenalty);

        assertEquals(13.0, travelTime.getLinkTravelTime(link, 100.0, null, null), 0.0);
    }
}
