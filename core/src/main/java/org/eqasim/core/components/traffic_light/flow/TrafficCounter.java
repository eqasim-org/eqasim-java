package org.eqasim.core.components.flow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrafficCounter implements LinkEnterEventHandler {

    private final TimeBinManager timeBinManager;
    private final Network network;

    private final IdMap<Link, List<Double>> counts = new IdMap<>(Link.class);
    private final static Logger logger = LogManager.getLogger(TrafficCounter.class);

    public TrafficCounter(Network network, TimeBinManager timeBinManager) {
        this.timeBinManager = timeBinManager;
        this.network = network;

        initializeFlowMap();
    }

    public void initializeFlowMap() {
        logger.info("Initializing Traffic Counts Map");
        counts.clear();
        for (Id<Link> linkId : network.getLinks().keySet()) {
            counts.put(linkId, new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
        }
    }

    @Override
    public synchronized void handleEvent(LinkEnterEvent event) {
        processEnterLink(event.getTime(), event.getLinkId());
    }

    public void processEnterLink(double time, Id<Link> linkId) {
        if (time >= timeBinManager.getStartTime() || time <= timeBinManager.getEndTime()) {
            int idx = timeBinManager.getBinIndex(time);
            List<Double> linkCounts = counts.get(linkId);
            linkCounts.set(idx, linkCounts.get(idx) + 1);
        }
    }

    public IdMap<Link, List<Double>> getCounts() {
        return counts;
    }

    @Override
    public void reset(int iteration) {
        logger.info("Resetting traffic counts for iteration {}", iteration);
        initializeFlowMap();
    }
}
