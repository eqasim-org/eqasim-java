package org.eqasim.core.components.network_calibration.cost_calibration;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class TestPenaltiesAdapterInitialization {
    @Test
    public void explicitFixedCsvOverridesArePersistedForFutureAttributeOnlyRuns() throws Exception {
        var csv = java.nio.file.Files.createTempFile("fixed-penalties", ".csv");
        try {
            java.nio.file.Files.writeString(csv, "linkCategory;isUrban;specialRegion;penalty\n2;true;0;0.2\n");
            var config = new NetworkCalibrationConfigGroup();
            config.setActivate(true);
            config.setObjective("penalty");
            var cost = config.getCostCalibrationConfigGroup();
            cost.setPenaltiesFile(csv.toString());
            var network = NetworkUtils.createNetwork();
            var from = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
            var to = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId("road"), from, to, 100, 10, 1000, 1);
            link.setAllowedModes(java.util.Set.of("car"));
            link.getAttributes().putAttribute("osm:way:highway", "primary");
            link.getAttributes().putAttribute("municipalityType", "urban");
            link.getAttributes().putAttribute("penalty", 0.1);
            var categorizer = new LinkCategorizer(network, config);
            var adapter = new PenaltiesAdapter(network,
                    () -> { throw new AssertionError("No counts required"); },
                    () -> { throw new AssertionError("No flow data required"); },
                    config, cost, mock(OutputDirectoryHierarchy.class), new EqasimConfigGroup(), categorizer,
                    new PenaltyKeyManager(config, cost, network, categorizer), new PenaltyManager(config, cost));
            assertEquals(2, adapter.computePenalty(link), 1e-12);
            adapter.notifyIterationEnds(new org.matsim.core.controler.events.IterationEndsEvent(null, 0, true));
            assertEquals(0.2, (double) link.getAttributes().getAttribute("penalty"), 0);
        } finally {
            java.nio.file.Files.deleteIfExists(csv);
        }
    }

    @Test
    public void fixedPenaltiesUseIndividualAttributesAndApplyRoadMultipliersOnce() {
        var config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setObjective("penalty");
        config.getCostCalibrationConfigGroup().setSpecialRegionPath("missing-regions.geojson");
        config.getCostCalibrationConfigGroup().setRampFactor(2);
        config.getCostCalibrationConfigGroup().setTrunkFactor(3);
        var network = NetworkUtils.createNetwork();
        var from = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0, 0));
        var to = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100, 0));
        for (String type : new String[] {"primary", "primary_link", "trunk", "other", "missing"}) {
            var link = NetworkUtils.createAndAddLink(network, Id.createLinkId(type), from, to, 100, 10, 1000, 1);
            link.setAllowedModes(java.util.Set.of("car"));
            link.getAttributes().putAttribute("osm:way:highway", type);
            link.getAttributes().putAttribute("municipalityType", "urban");
            if (!type.equals("missing")) link.getAttributes().putAttribute("penalty", 0.1);
        }
        var cost = config.getCostCalibrationConfigGroup();
        var categorizer = new LinkCategorizer(network, config);
        var manager = spy(new PenaltyManager(config, cost));
        var adapter = new PenaltiesAdapter(network,
                () -> { throw new AssertionError("Fixed penalties must not load counts"); },
                () -> { throw new AssertionError("Fixed penalties must not load flow data"); },
                config, cost, mock(OutputDirectoryHierarchy.class), new EqasimConfigGroup(), categorizer,
                new PenaltyKeyManager(config, cost, network, categorizer), manager);
        assertEquals(1, adapter.computePenalty(network.getLinks().get(Id.createLinkId("primary"))), 1e-12);
        assertEquals(2, adapter.computePenalty(network.getLinks().get(Id.createLinkId("primary_link"))), 1e-12);
        assertEquals(3, adapter.computePenalty(network.getLinks().get(Id.createLinkId("trunk"))), 1e-12);
        assertEquals(1, adapter.computePenalty(network.getLinks().get(Id.createLinkId("other"))), 1e-12);
        assertEquals(0, adapter.computePenalty(network.getLinks().get(Id.createLinkId("missing"))), 0);
        var link = network.getLinks().get(Id.createLinkId("primary"));
        link.getAttributes().putAttribute("penalty", "0.2");
        assertEquals(2, adapter.computePenalty(link), 1e-12);
        adapter.disable();
        assertEquals(0, adapter.computePenalty(link), 0);
        adapter.enable();
        assertEquals(2, adapter.computePenalty(link), 1e-12);
        verifyNoInteractions(manager);
    }

    @Test
    public void explicitResetIgnoresNetworkAndCsvInitialPenalties() {
        NetworkCalibrationConfigGroup config = new NetworkCalibrationConfigGroup();
        config.setActivate(true);
        config.setCalibrate(true);
        CostCalibrationConfigGroup costConfig = config.getCostCalibrationConfigGroup();
        costConfig.setActivate(true);
        costConfig.setPenaltiesFile("missing-initial-penalties.csv");
        costConfig.setResetPenaltiesToZeros(true);

        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId("from"), new Coord(0.0, 0.0));
        var to = factory.createNode(Id.createNodeId("to"), new Coord(100.0, 0.0));
        network.addNode(from);
        network.addNode(to);
        var link = factory.createLink(Id.createLinkId("link"), from, to);
        link.getAttributes().putAttribute("penalty", 0.25);
        network.addLink(link);

        PenaltyGroupKey key = new PenaltyGroupKey(1, false, 0);
        LinkCategorizer categorizer = mock(LinkCategorizer.class);
        when(categorizer.getPenaltyGroupKey(link)).thenReturn(key);
        PenaltyKeyManager keyManager = mock(PenaltyKeyManager.class);
        when(keyManager.toCalibrationKey(key)).thenReturn(key);
        PenaltyManager manager = new PenaltyManager(config, costConfig);
        EqasimConfigGroup eqasimConfig = mock(EqasimConfigGroup.class);
        when(eqasimConfig.getSampleSize()).thenReturn(1.0);

        new PenaltiesAdapter(
                network, () -> mock(CountsProcessor.class), () -> mock(FlowProcessor.class),
                config, costConfig, mock(OutputDirectoryHierarchy.class), eqasimConfig,
                categorizer, keyManager, manager);

        assertEquals(0.0, manager.getPenalty(key), 1.0e-12);
    }
}
