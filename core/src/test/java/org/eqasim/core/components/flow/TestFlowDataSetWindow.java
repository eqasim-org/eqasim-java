package org.eqasim.core.components.flow;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TestFlowDataSetWindow {
    private FlowDataSet dataSet;
    private Id<Link> linkId;

    @Before
    public void setUp() {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId("from-window"), new Coord(0.0, 0.0));
        var to = factory.createNode(Id.createNodeId("to-window"), new Coord(100.0, 0.0));
        network.addNode(from);
        network.addNode(to);
        linkId = Id.createLinkId("window-link");
        network.addLink(factory.createLink(linkId, from, to));

        FlowConfigGroup config = new FlowConfigGroup();
        config.setStartTime(0.0);
        config.setEndTime(9000.0);
        config.setBinSize(3600.0);
        config.setBeta(1.0);

        FlowBinManager binManager = new FlowBinManager(config);
        dataSet = new FlowDataSet(network, binManager, config.getBeta());
        dataSet.getFlowMap().put(linkId, new float[]{100.0F, 200.0F, 50.0F});
    }

    @Test
    public void convertsSingleAndMultipleBinsToHourlyFlow() {
        assertEquals(100.0F, dataSet.getFlow_v_h(linkId, 1800.0), 1.0e-5F);
        assertEquals(300.0F, dataSet.getFlowInWindow(linkId, 3600.0, 7200.0), 1.0e-5F);
        assertEquals(150.0F, dataSet.getFlow_v_h(linkId, 3600.0, 7200.0), 1.0e-5F);
    }

    @Test
    public void weightsPartialBinsByTheirExactOverlap() {
        assertEquals(150.0F, dataSet.getFlowInWindow(linkId, 3600.0, 3600.0), 1.0e-5F);
        assertEquals(150.0F, dataSet.getFlow_v_h(linkId, 3600.0, 3600.0), 1.0e-5F);
    }

    @Test
    public void usesCoveredDurationAtConfigurationBoundaries() {
        assertEquals(50.0F, dataSet.getFlowInWindow(linkId, 0.0, 3600.0), 1.0e-5F);
        assertEquals(100.0F, dataSet.getFlow_v_h(linkId, 0.0, 3600.0), 1.0e-5F);
        assertEquals(0.0F, dataSet.getFlow_v_h(linkId, -4000.0, 1000.0), 0.0F);
    }

    @Test
    public void checkTheLastBInBehavior() {
        assertEquals(50.0F, dataSet.getFlow_v_h(linkId, 8100.0), 1.0e-5F);
        assertEquals(100.0F, dataSet.getFlowInWindow(linkId, 8100.0, 3600.0), 1.0e-5F);
        assertEquals(100.0F, dataSet.getFlow_v_h(linkId, 8100.0, 600.0), 1.0e-5F);
    }

    @Test
    public void rejectsInvalidWindows() {
        assertThrows(IllegalArgumentException.class,
                () -> dataSet.getFlowInWindow(linkId, 1800.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> dataSet.getFlow_v_h(linkId, 1800.0, Double.NaN));
    }
}
