package org.eqasim.core.components.flow;

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
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class LinkFlowCounter implements LinkEnterEventHandler, IterationEndsListener, ShutdownListener {

    private final FlowBinManager flowBinManager;
    private final Network network;
    private final FlowDataSet flowDataSet;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final int writeFlowInterval;
    private final int numberOfBins;
    private final double startTime;
    private final double endTime;

    private final IdMap<Link, double[]> counts = new IdMap<>(Link.class);
    private final static Logger logger = LogManager.getLogger(LinkFlowCounter.class);
    private final String FLOW_FILE = "traffic_flow.csv";

    public LinkFlowCounter(Network network, FlowDataSet flowDataSet, FlowBinManager flowBinManager,
                           OutputDirectoryHierarchy outputHierarchy, DelaysConfigGroup delaysConfigGroup) {
        this.flowBinManager = flowBinManager;
        this.network = network;
        this.flowDataSet = flowDataSet;
        this.outputHierarchy = outputHierarchy;
        this.writeFlowInterval = delaysConfigGroup.getWriteFlowInterval();
        this.numberOfBins = flowBinManager.getNumberOfBins();
        this.startTime = flowBinManager.getStartTime();
        this.endTime = flowBinManager.getEndTime();
        initializeCountsMap();
    }

    public void initializeCountsMap() {
        logger.info("Initializing Traffic Counts Map");
        counts.clear();
        // TODO: do not consider non car links
        for (Id<Link> linkId : network.getLinks().keySet()) {
            counts.put(linkId, flowDataSet.getZeros(numberOfBins));
        }
    }

    @Override
    public synchronized void handleEvent(LinkEnterEvent event) {
        processEnterLink(event.getTime(), event.getLinkId());
    }

    public void processEnterLink(double time, Id<Link> linkId) {
        if (flowBinManager.timeInBounds(time)) {
            int idx = flowBinManager.getBinIndex(time);
            double[] linkCounts = counts.get(linkId);
            linkCounts[idx] += 1.0;
        }
    }

    public int getNumberOfLinks() {
        return counts.size();
    }

    public IdMap<Link, double[]> getCountsArray() {
        return counts;
    }

    public double[] getLinkCountsArray(Id<Link> linkId) {
        return counts.get(linkId);
    }

    public double getLinkCounts(Id<Link> linkId, double time) {
        double[] linkCounts = counts.get(linkId);
        int binIdx = flowBinManager.getBinIndex(time);
        return linkCounts[binIdx];
    }

    @Override
    public void reset(int iteration) {
        logger.info("Resetting traffic counts for iteration {}", iteration);
        counts.values().forEach(countsArray -> Arrays.fill(countsArray, 0.0));
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        flowDataSet.updateFlow(event.getIteration(), this);
        writeFlowDataIfRequired(event);
    }

    private void writeFlowDataIfRequired(IterationEndsEvent event) {
        if (writeFlowInterval > 0 && (event.getIteration() % writeFlowInterval == 0 || event.isLastIteration())) {
            File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));
            writeFlow(outputFile);
        }
    }

    @Override
    public void notifyShutdown(ShutdownEvent event) {
        File outputFile = new File(outputHierarchy.getOutputFilenameWithOutputPrefix(FLOW_FILE));
        writeFlow(outputFile);
    }

    private void writeFlow(File outputFile) {
        logger.info("Writing FlowDataSet data to {}...", outputFile);
        try {
            flowDataSet.exportToCSV(String.valueOf(outputFile));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("  Done");
    }
}
