package org.eqasim.core.components.traffic_light.flow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SequencedCollection;

public class TrafficCounter implements LinkEnterEventHandler, IterationEndsListener {

    private final TimeBinManager timeBinManager;
    private final Network network;
    private final FlowDataSet flowDataSet;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final int writeFlowInterval;

    private final IdMap<Link, List<Double>> counts = new IdMap<>(Link.class);
    private final static Logger logger = LogManager.getLogger(TrafficCounter.class);
    private final String FLOW_FILE = "traffic_flow.csv";

    public TrafficCounter(Network network, FlowDataSet flowDataSet, TimeBinManager timeBinManager,
                          OutputDirectoryHierarchy outputHierarchy, DelaysConfigGroup delaysConfigGroup) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.flowDataSet = flowDataSet;
        this.outputHierarchy = outputHierarchy;
        this.writeFlowInterval = delaysConfigGroup.getWriteFlowInterval();
        initializeCountsMap();
    }

    public void initializeCountsMap() {
        logger.info("Initializing Traffic Counts Map");
        counts.clear();
        // TODO: do not consider non car links
        for (Id<Link> linkId : network.getLinks().keySet()) {
            counts.put(linkId, new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
        }
    }

    @Override
    public synchronized void handleEvent(LinkEnterEvent event) {
        processEnterLink(event.getTime(), event.getLinkId());
    }

    public void processEnterLink(double time, Id<Link> linkId) {
        if (time >= timeBinManager.getStartTime() && time < timeBinManager.getEndTime()) {
            int idx = timeBinManager.getBinIndex(time);
            List<Double> linkCounts = counts.get(linkId);
            linkCounts.set(idx, linkCounts.get(idx) + 1);
        }
    }

    public IdMap<Link, List<Double>> getCounts() {
        return counts;
    }

    public List<Double> getLinkCounts(Id<Link> linkId) {
        return counts.get(linkId);
    }

    public Double getLinkCounts(Id<Link> linkId, double time) {
        if (time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime()) {
            throw new IllegalArgumentException("Time " + time + " is out of bounds (" +
                    timeBinManager.getStartTime() + " - " + timeBinManager.getEndTime() + ")");
        }
        List<Double> linkCounts = counts.get(linkId);
        int binIdx = timeBinManager.getBinIndex(time);
        return linkCounts.get(binIdx);
    }

    @Override
    public void reset(int iteration) {
        logger.info("Resetting traffic counts for iteration {}", iteration);
        counts.values().forEach(countsList -> Collections.fill(countsList, 0.0));
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (event.getIteration()==0) {
            // init the flow map at the first iteration
            flowDataSet.initializeFlowMap();
        }

        flowDataSet.updateFlow(event.getIteration(), this);
        writeFlowDataIfRequired(event);

    }

    private void writeFlowDataIfRequired(IterationEndsEvent event) {
        if (writeFlowInterval > 0 && (event.getIteration() % writeFlowInterval == 0 || event.isLastIteration())) {
            File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));

            logger.info("Writing FlowDataSet data to " + outputFile.toString() + "...");
            try {
                flowDataSet.exportToCSV(String.valueOf(outputFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("  Done");

        }
    }
}
