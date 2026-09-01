package org.eqasim.core.components.flow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class LinkFlowCounter implements LinkEnterEventHandler, IterationEndsListener, ShutdownListener {

    private final FlowBinManager flowBinManager;
    private final Network network;
    private final VehiclePcuLookup vehiclePcuLookup;
    private final double sampleSize;
    private final FlowDataSet flowDataSet;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final int writeFlowInterval;
    private final int numberOfBins;
    private final boolean isActivated;
    private final IdMap<Link, float[]> counts = new IdMap<>(Link.class);
    private float[] dailyCounts;
    private final static Logger logger = LogManager.getLogger(LinkFlowCounter.class);
    private final String FLOW_FILE = "traffic_flow.csv";

    public LinkFlowCounter(Network network, FlowDataSet flowDataSet, FlowBinManager flowBinManager,
                           OutputDirectoryHierarchy outputHierarchy, FlowConfigGroup config,
                           VehiclePcuLookup vehiclePcuLookup, double sampleSize) {
        this.flowBinManager = flowBinManager;
        this.network = network;
        this.vehiclePcuLookup = vehiclePcuLookup;
        this.sampleSize = sampleSize;
        this.flowDataSet = flowDataSet;
        this.outputHierarchy = outputHierarchy;
        this.writeFlowInterval = config.getWriteFlowInterval();
        this.isActivated = config.isActivated();
        this.numberOfBins = flowBinManager.getNumberOfBins();
        initializeCountsMap();
    }

    public void initializeCountsMap() {
        logger.info("Initializing Traffic Counts Map");
        counts.clear();
        int maximumLinkIndex = network.getLinks().keySet().stream()
                .mapToInt(Id::index)
                .max()
                .orElse(-1);
        dailyCounts = new float[maximumLinkIndex + 1];
        // TODO: do not consider non car links
        for (Id<Link> linkId : network.getLinks().keySet()) {
            counts.put(linkId, new float[numberOfBins]);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        double pcu = vehiclePcuLookup.getPcu(event.getVehicleId());
        processEnterLink(event.getTime(), event.getLinkId(), pcu);
    }

    public void processEnterLink(double time, Id<Link> linkId, double pcu) {
        if (isActivated && pcu > 1e-6 && flowBinManager.timeInBounds(time)) {
            int idx = flowBinManager.getBinIndex(time);
            float[] linkCounts = counts.get(linkId);
            float dailyCountIncrement = FlowUtils.getCountValue(pcu, sampleSize);
            synchronized (linkCounts) {
                linkCounts[idx] += (float) pcu;
                dailyCounts[linkId.index()] += dailyCountIncrement;
            }
        }
    }

    public int getNumberOfLinks() {
        return counts.size();
    }

    public IdMap<Link, float[]> getCountsArray() {
        return counts;
    }

    public float[] getLinkCountsArray(Id<Link> linkId) {
        return counts.get(linkId);
    }

    public float getDailyCounts(Id<Link> linkId) {
        return dailyCounts[linkId.index()];
    }

    public float getLinkCounts(Id<Link> linkId, double time) {
        float[] linkCounts = getLinkCountsArray(linkId);
        int binIdx = flowBinManager.getBinIndex(time);
        return linkCounts[binIdx];
    }

    @Override
    public void reset(int iteration) {
        logger.info("Resetting traffic counts for iteration {}", iteration);
        counts.values().forEach(countsArray -> Arrays.fill(countsArray, 0.0F));
        Arrays.fill(dailyCounts, 0.0F);
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
            exportDailyCountsToCSV(String.valueOf(outputFile).replace(".csv", "_daily_counts.csv"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        logger.info("  Done");
    }

    private void exportDailyCountsToCSV(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            // Write header: linkId, dailyCount
            writer.write("linkId;dailyCount\n");

            // Write each link's daily count as a row
            for (Id<Link> linkId : network.getLinks().keySet()) {
                double dailyCount = Math.round(dailyCounts[linkId.index()] * 100.0) / 100.0;

                String row = String.format("%s;%f\n", linkId.toString(), dailyCount);
                writer.write(row);
            }
        }
    }
}
