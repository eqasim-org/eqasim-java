package org.eqasim.core.components.flow;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;

public class FlowDataSet {

    private final FlowBinManager flowBinManager;
    private final float beta;
    private final Network network;
    private final IdMap<Link, float[]> flowMap = new IdMap<>(Link.class);

    private int updatesCounter = 0;
    private final int numberOfBins;
    private final float flowRatio;
    private final double binSize;
    private static final Logger logger = LogManager.getLogger(FlowDataSet.class);

    public FlowDataSet(Network network, FlowBinManager flowBinManager, double beta) {
        this.network = network;
        this.flowBinManager = flowBinManager;
        this.beta = (float) beta;
        this.numberOfBins = flowBinManager.getNumberOfBins();
        this.binSize = flowBinManager.getBinSize();
        this.flowRatio = (float) (3600.0 / flowBinManager.getBinSize()); // This is the ratio to convert from veh/bin to veh/h
        initializeFlowMap();
    }

    public float getFlow_v_h(Id<Link> linkId, double time) {
        float flow = getFlow(linkId, time);
        return flow * flowRatio; // Convert from veh/bin to veh/h
    }

    public float getFlow_v_h(Id<Link> linkId, double time, double aggregationWindow) {
        WindowFlow windowFlow = aggregateWindow(linkId, time, aggregationWindow);
        return windowFlow.duration > 0.0
                ? (float) (windowFlow.vehicleCount * 3600.0 / windowFlow.duration)
                : 0.0F;
    }

    public float getFlow(Id<Link> linkId, double time) {
        // Return the number of vehicles stored in the bin containing time.
        float[] flows = flowMap.get(linkId);
        int binIdx = flowBinManager.getBinIndex(time);
        return flows[binIdx];
    }

    /**
     * Returns the estimated number of vehicles inside a window centered on {@code time}.
     * Partial bins are weighted by their overlap with the window.
     */
    public float getFlowInWindow(Id<Link> linkId, double time, double aggregationWindow) {
        return (float) aggregateWindow(linkId, time, aggregationWindow).vehicleCount;
    }

    private WindowFlow aggregateWindow(Id<Link> linkId, double time, double aggregationWindow) {
        if (!Double.isFinite(time)) {
            throw new IllegalArgumentException("time must be finite. Provided: " + time);
        }
        if (!Double.isFinite(aggregationWindow) || aggregationWindow <= 0.0) {
            throw new IllegalArgumentException("aggregationWindow must be finite and > 0. Provided: " + aggregationWindow);
        }

        double windowStart = time - aggregationWindow / 2.0;
        double windowEnd = time + aggregationWindow / 2.0;
        double coveredStart = Math.max(windowStart, flowBinManager.getStartTime());
        double coveredEnd = Math.min(windowEnd, flowBinManager.getEndTime());

        if (coveredEnd <= coveredStart) {
            return new WindowFlow(0.0, 0.0);
        }

        float[] flows = flowMap.get(linkId);
        double flowStart = flowBinManager.getStartTime();
        int firstBin = Math.max(0, (int) Math.floor((coveredStart - flowStart) / binSize));
        int lastBinExclusive = Math.min(numberOfBins,
                (int) Math.ceil((coveredEnd - flowStart) / binSize));
        double vehicleCount = 0.0;

        for (int bin = firstBin; bin < lastBinExclusive; bin++) {
            double binStart = flowStart + bin * binSize;
            double binEnd = Math.min(binStart + binSize, flowBinManager.getEndTime());
            double overlap = Math.min(coveredEnd, binEnd) - Math.max(coveredStart, binStart);

            if (overlap > 0.0) {
                // Counts are assumed to be uniformly distributed within a bin.
                vehicleCount += flows[bin] * overlap / (binEnd - binStart);
            }
        }

        return new WindowFlow(vehicleCount, coveredEnd - coveredStart);
    }

    private record WindowFlow(double vehicleCount, double duration) {
    }

    public void initializeFlowMap() {
        logger.info("Initializing FlowDataSet Map");
        flowMap.clear();
        for (Id<Link> linkId : network.getLinks().keySet()) {
            flowMap.put(linkId, new float[numberOfBins]);
        }
    }

    private float getBetaEffective() {
        if (updatesCounter==0) {
            return 0.0F; // First update uses only new data
        } else if (updatesCounter<=5) {
            return 0.2F; // Use a lower beta for the first few updates to adapt quickly
        }
        return this.beta;
    }

    public void updateFlow(int iteration, LinkFlowCounter counts) {
        logger.info("Iteration {}: Updating iteration flow data", iteration);

        if (counts.getNumberOfLinks() != network.getLinks().size()) {
            logger.error("FlowDataSet size mismatch. Expected: {}, Got: {}", network.getLinks().size(), counts.getNumberOfLinks());
            return;
        }

        float betaEffective = getBetaEffective();
        float oneMinusBetaEffective = 1.0F - betaEffective;

        for (Id<Link> linkId : network.getLinks().keySet()) {
            float[] existingFlow = flowMap.get(linkId);
            float[] newFlow = counts.getLinkCountsArray(linkId);

            for (int i = 0; i < numberOfBins; i++) {
                existingFlow[i] = betaEffective * existingFlow[i] + oneMinusBetaEffective * newFlow[i];
            }
        }

        updatesCounter++;
    }

    public void exportToCSV(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            int numberOfBins = flowBinManager.getNumberOfBins();
            // Write header: linkId, bin0, bin1, bin2, ...
            StringBuilder header = new StringBuilder("linkId");
            for (int bin = 0; bin < numberOfBins; bin++) {
                header.append(String.format(";bin%d", bin));
            }
            writer.write(header.toString() + "\n");

            // Write each link's flows as a row
            for (Map.Entry<Id<Link>, float[]> entry : flowMap.entrySet()) {
                Id<Link> linkId = entry.getKey();
                float[] binFlows = entry.getValue();

                StringBuilder row = new StringBuilder(linkId.toString());
                for (int bin = 0; bin < numberOfBins; bin++) {
                    row.append(String.format(";%.1f", binFlows[bin]));
                }
                writer.write(row.toString() + "\n");
            }
        }
    }

    public double getAverageFlow() {
        double totalFlow = 0.0;
        int count = 0;

        for (float[] flows : flowMap.values()) {
            for (float flow : flows) {
                totalFlow += flow;
                count++;
            }
        }

        return count > 0 ? (totalFlow / count) * flowRatio : 0.0;
    }

    public IdMap<Link, float[]> getFlowMap() {
        return flowMap;
    }

    public int getNumberOfLinks() {
        return flowMap.size();
    }

    public void clear() {
        flowMap.forEach(k -> Arrays.fill(k, 0.0F));
        updatesCounter = 0;
    }

    public IdMap<Link, double[]> getFlowBinMapInDifferentBins(double startTime, double endTime, double interval) {
        // This will not necessarly returns number of vehicles per hour, but number of vehicles within the binSize period
        int numberOfBins = flowBinManager.getNumberOfBins(startTime, endTime, interval);
        double[] binCenters = flowBinManager.getBinsCenters(numberOfBins, startTime, interval);

        IdMap<Link, double[]> newFlowMap = new IdMap<>(Link.class);
        for (Id<Link> linkId : flowMap.keySet()) {
            double[] newFlows = new double[numberOfBins];
            for (int i = 0; i < numberOfBins; i++) {
                double midTime = binCenters[i];
                newFlows[i] = getFlowInWindow(linkId, midTime, interval);
            }
            newFlowMap.put(linkId, newFlows);
        }

        return newFlowMap;
    }
}
