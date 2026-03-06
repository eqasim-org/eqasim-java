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
    private final double beta;
    private final Network network;
    private final IdMap<Link, double[]> flowMap = new IdMap<>(Link.class);

    private int updatesCounter = 0;
    private final int numberOfBins;
    private final double flowRatio;
    private static final Logger logger = LogManager.getLogger(FlowDataSet.class);

    public FlowDataSet(Network network, FlowBinManager flowBinManager, double beta) {
        this.network = network;
        this.flowBinManager = flowBinManager;
        this.beta = beta;
        this.numberOfBins = flowBinManager.getNumberOfBins();
        this.flowRatio = 3600.0 / flowBinManager.getBinSize();
        initializeFlowMap();
    }

    public double getFlow(Id<Link> linkId, double time) {
        // this method should always return flow in veh/h, so we need to multiply the stored flow (which is in veh/bin) by the flowRatio (veh/h per veh/bin)
        double[] flows = flowMap.get(linkId);
        int binIdx = flowBinManager.getBinIndex(time);
        return flows[binIdx] * flowRatio;
    }

    public void initializeFlowMap() {
        logger.info("Initializing FlowDataSet Map");
        flowMap.clear();
        for (Id<Link> linkId : network.getLinks().keySet()) {
            flowMap.put(linkId, getZeros(numberOfBins));
        }
    }

    public double[] getZeros(int size) {
        double[] zeros = new double[size];
        Arrays.fill(zeros, 0.0);
        return zeros;
    }

    private double getBetaEffective() {
        if (updatesCounter==0) {
            return 0.0; // First update uses only new data
        } else if (updatesCounter<=5) {
            return 0.2; // Use a lower beta for the first few updates to adapt quickly
        }
        return this.beta;
    }

    public void updateFlow(int iteration, LinkFlowCounter counts) {
        logger.info("Iteration {}: Updating iteration flow data", iteration);

        if (counts.getNumberOfLinks() != network.getLinks().size()) {
            logger.error("FlowDataSet size mismatch. Expected: {}, Got: {}", network.getLinks().size(), counts.getNumberOfLinks());
            return;
        }

        double betaEffective = getBetaEffective();
        double oneMinusBetaEffective = 1.0 - betaEffective;

        for (Id<Link> linkId : network.getLinks().keySet()) {
            double[] existingFlow = flowMap.get(linkId);
            double[] newFlow = counts.getLinkCountsArray(linkId);

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
            for (Map.Entry<Id<Link>, double[]> entry : flowMap.entrySet()) {
                Id<Link> linkId = entry.getKey();
                double[] binFlows = entry.getValue();

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

        for (double[] flows : flowMap.values()) {
            for (double flow : flows) {
                totalFlow += flow;
                count++;
            }
        }

        return count > 0 ? (totalFlow / count) * flowRatio : 0.0;
    }

    public IdMap<Link, double[]> getFlowMap() {
        return flowMap;
    }

    public int getNumberOfLinks() {
        return flowMap.size();
    }

    public void clear() {
        flowMap.forEach(k -> Arrays.fill(k, 0.0));
        updatesCounter = 0;
    }
}
