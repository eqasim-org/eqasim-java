package org.matsim.dsim.simulation.net;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.dsim.DSimConfigGroup;
import org.matsim.dsim.NetworkDecomposition;
import org.matsim.vehicles.VehicleUtils;

public class TestDsimLinkAdapter {
    @Test public void bikeLayerDoesNotKeepCapacityDiagnostics() {
        var network = NetworkUtils.createNetwork();
        var from = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var to = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
        from.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        to.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
        var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("__dsim_bike_link__ab"),
                from, to, 100, 10, 3600, 1);
        var config = new DSimConfigGroup();
        config.setTrafficDynamics(org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics.queue);
        SimLink.create(link, new SimNode(to.getId()), config, 7.5, 0, ignored -> {}, ignored -> {}, null);
        assertNull(link.getAttributes().getAttribute("maxInflowUsedInQsim"));
        assertNull(link.getAttributes().getAttribute("storageCapacityUsedInQsim"));
    }

    @Test public void delayAppliedOnceOnDownstreamOwner() {
        for (boolean boundary : new boolean[] {false, true}) {
            var network = NetworkUtils.createNetwork();
            var from = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
            var to = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
            from.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, boundary ? 1 : 0);
            to.getAttributes().putAttribute(NetworkDecomposition.PARTITION_ATTR_KEY, 0);
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("ab"), from, to, 100, 10, 3600, 1);
            var config = new DSimConfigGroup();
            config.setTrafficDynamics(org.matsim.core.config.groups.QSimConfigGroup.TrafficDynamics.queue);
            var sim = SimLink.create(link, new SimNode(to.getId()), config, 7.5, 0, l -> {}, n -> {}, null);
            var type = VehicleUtils.getFactory().createVehicleType(Id.create("car", org.matsim.vehicles.VehicleType.class));
            var vehicle = new QVehicleImpl(VehicleUtils.createVehicle(Id.createVehicleId("v"), type));
            AtomicInteger calls = new AtomicInteger();
            sim.setLinkTravelTime((v, t) -> { calls.incrementAndGet(); return 10 + 7; });
            sim.pushVehicle(vehicle, SimLink.LinkPosition.QStart, 100);
            assertEquals(117, vehicle.getEarliestLinkExitTime(), 0);
            assertEquals(1, calls.get());
            // Waiting to become eligible must never reapply a stateful intersection penalty.
            sim.doSimStep(101);
            sim.doSimStep(116);
            assertEquals(1, calls.get());
        }
    }
}
