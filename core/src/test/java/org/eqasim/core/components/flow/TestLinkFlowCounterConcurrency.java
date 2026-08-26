package org.eqasim.core.components.flow;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.network.NetworkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class TestLinkFlowCounterConcurrency {
    @Test
    public void countsParallelUpdatesToTheSameLinkWithoutBoxingOrLostUpdates() throws Exception {
        Network network = NetworkUtils.createNetwork();
        var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId("from"), new Coord(0.0, 0.0));
        var to = factory.createNode(Id.createNodeId("to"), new Coord(100.0, 0.0));
        network.addNode(from);
        network.addNode(to);
        Id<Link> linkId = Id.createLinkId("link");
        network.addLink(factory.createLink(linkId, from, to));

        FlowConfigGroup config = new FlowConfigGroup();
        config.setStartTime(0.0);
        config.setEndTime(3600.0);
        config.setBinSize(3600.0);
        config.setActivate(true);

        FlowBinManager binManager = new FlowBinManager(config);
        FlowDataSet dataSet = new FlowDataSet(network, binManager, config.getBeta());
        LinkFlowCounter counter = new LinkFlowCounter(
                network, dataSet, binManager, mock(OutputDirectoryHierarchy.class), config,
                mock(VehiclePcuLookup.class), 1.0);

        int threadCount = 4;
        int eventsPerThread = 25_000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        try {
            for (int thread = 0; thread < threadCount; thread++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int event = 0; event < eventsPerThread; event++) {
                        counter.processEnterLink(1800.0, linkId, 1.0);
                    }
                    return null;
                }));
            }

            start.countDown();
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        float expected = threadCount * eventsPerThread;
        assertEquals(expected, counter.getLinkCounts(linkId, 1800.0), 0.0F);
        assertEquals(expected, counter.getDailyCounts(linkId), 0.0F);

        counter.reset(1);
        assertEquals(0.0F, counter.getLinkCounts(linkId, 1800.0), 0.0F);
        assertEquals(0.0F, counter.getDailyCounts(linkId), 0.0F);
    }
}
