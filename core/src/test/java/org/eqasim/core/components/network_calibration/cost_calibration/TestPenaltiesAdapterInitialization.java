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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestPenaltiesAdapterInitialization {
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
