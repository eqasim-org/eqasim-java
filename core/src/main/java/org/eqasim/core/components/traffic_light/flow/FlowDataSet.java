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

        initializeFlowMap();
    }

    private void initializeFlowMap() {
        logger.info("Initializing FlowDataSet Map");
        flowMap.clear();
        for (Id<Link> linkId : network.getLinks().keySet()) {
            flowMap.put(linkId, new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
        }
    }

    public void updateFlow(int iteration, IdMap<Link, List<Double>> flow) {
        logger.info("Iteration {}: Adding iteration flow data", iteration);

        if (flow.size() != network.getLinks().size()) {
            logger.error("FlowDataSet size mismatch. Expected: {}, Got: {}", network.getLinks().size(), flow.size());
            return;
        }

        double betaEffective = (updatesCounter == 0) ? 1.0 : this.beta;

        for (Id<Link> linkId : network.getLinks().keySet()) {
            List<Double> existingFlow = flowMap.get(linkId);
            List<Double> newFlow = flow.get(linkId);

            for (int i = 0; i < timeBinManager.getNumberOfBins(); i++) {
                double updated = betaEffective * newFlow.get(i) + (1.0 - betaEffective) * existingFlow.get(i);
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


}
