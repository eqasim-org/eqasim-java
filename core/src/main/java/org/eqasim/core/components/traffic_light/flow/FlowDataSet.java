package org.eqasim.core.components.traffic_light.flow;


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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;



public class FlowDataSet {

    private final TimeBinManager timeBinManager;
    private final double beta;
    private final Network network;
    private final IdMap<Link, List<Double>> flowMap = new IdMap<>(Link.class);

    private int updatesCounter = 0;

    private static final Logger logger = LogManager.getLogger(FlowDataSet.class);

    public FlowDataSet(Network network, TimeBinManager timeBinManager, double beta) {
        this.network = network;
        this.timeBinManager = timeBinManager;
        this.beta = beta;
    }

    public double getFlow(Id<Link> linkId, double time) {
        if (time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime()) {
            throw new IllegalArgumentException("Time " + time + " is out of bounds (" +
                    timeBinManager.getStartTime() + " - " + timeBinManager.getEndTime() + ")");
        }
        List<Double> flows = flowMap.get(linkId);
        int binIdx = timeBinManager.getBinIndex(time);
        return flows.get(binIdx);
    }

    public void initializeFlowMap() {
        logger.info("Initializing FlowDataSet Map");
        flowMap.clear();
        for (Id<Link> linkId : network.getLinks().keySet()) {
            flowMap.put(linkId, new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
        }
    }

    private double getBetaEffective() {
        if (updatesCounter==0) {
            return 0.0; // First update uses only new data
        } else if (updatesCounter<=5) {
            return 0.2; // Use a lower beta for the first few updates to adapt quickly
        }
        return this.beta;
    }

    public void updateFlow(int iteration, TrafficCounter counts) {
        logger.info("Iteration {}: Updating iteration flow data", iteration);

        if (counts.getCounts().size() != network.getLinks().size()) {
            logger.error("FlowDataSet size mismatch. Expected: {}, Got: {}", network.getLinks().size(), counts.getCounts().size());
            return;
        }

        double betaEffective = getBetaEffective();

        for (Id<Link> linkId : network.getLinks().keySet()) {
            List<Double> existingFlow = flowMap.get(linkId);
            List<Double> newFlow = counts.getLinkCounts(linkId);

            for (int i = 0; i < timeBinManager.getNumberOfBins(); i++) {
                double updated = betaEffective * existingFlow.get(i) + (1.0 - betaEffective) * newFlow.get(i);
                existingFlow.set(i, updated);
            }
        }

        updatesCounter++;
    }

    public void exportToCSV(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            int numberOfBins = timeBinManager.getNumberOfBins();
            // Write header: linkId, bin0, bin1, bin2, ...
            StringBuilder header = new StringBuilder("linkId");
            for (int bin = 0; bin < numberOfBins; bin++) {
                header.append(String.format(";bin%d", bin));
            }
            writer.write(header.toString() + "\n");

            // Write each link's flows as a row
            for (Map.Entry<Id<Link>, List<Double>> entry : flowMap.entrySet()) {
                Id<Link> linkId = entry.getKey();
                List<Double> binFlows = entry.getValue();

                StringBuilder row = new StringBuilder(linkId.toString());
                for (int bin = 0; bin < numberOfBins; bin++) {
                    row.append(String.format(";%.1f", binFlows.get(bin)));
                }
                writer.write(row.toString() + "\n");
            }
        }
    }

    public double getAverageFlow() {
        return flowMap.values().stream().flatMap(List::stream).mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

}
