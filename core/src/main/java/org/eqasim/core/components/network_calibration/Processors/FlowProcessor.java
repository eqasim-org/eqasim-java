package org.eqasim.core.components.network_calibration.Processors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationUtils;
import org.eqasim.core.components.flow.TimeBinManager;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;

import java.io.BufferedWriter;
import java.util.*;

import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

public class FlowProcessor {

    private final static Logger logger = LogManager.getLogger(FlowProcessor.class);
    private final Network network;
    private final LinkFlowCounter linkFlowCounter;
    private final OutputDirectoryHierarchy outputHierarchy;
    private final CountsProcessor countsProcessor;

    private final Map<Integer, Double> flowPerCategory = new HashMap<>();
    private final Map<Integer, Integer> linksPerCategory = new HashMap<>();
    private final int indexOfStartingCounts;
    private final int indexOfEndingCounts;
    private final double totalNumberOfHours;

    public FlowProcessor(Network network, LinkFlowCounter linkFlowCounter, TimeBinManager timeBinManager,
                         CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy,
                         NetworkCalibrationConfigGroup config) {
        this.network = network;
        this.linkFlowCounter = linkFlowCounter;
        this.outputHierarchy = outputHierarchy;
        this.countsProcessor = countsProcessor;

        this.indexOfStartingCounts = timeBinManager.getBinIndex(config.getHourStartCounts() * 3600);
        this.indexOfEndingCounts = timeBinManager.getBinIndex(config.getHourEndCounts() * 3600);

        this.totalNumberOfHours = (timeBinManager.getBinInterval(indexOfEndingCounts)[1] - timeBinManager.getBinInterval(indexOfStartingCounts)[0])/3600.0;
    }


    public void updateAndSaveCounts(IterationEndsEvent iterationEndsEvent) {
        updateCounts();
        saveCounts(iterationEndsEvent.getIteration());
    }

    private void updateCounts() {
        // get the counts from the traffic counter
        IdMap<Link, List<Double>> countsMap = linkFlowCounter.getCounts();
        // aggregate the counts by link category
        for (Id<Link> linkId : countsMap.keySet()) {
            if (!countsProcessor.contains(linkId)) {
                continue; // skip links not in the counts processor (consider all when no counts file is provided)
            }

            Integer linkCategory = countsProcessor.getLinkCategory(linkId);
            if ((linkCategory==null) || (linkCategory == NetworkCalibrationUtils.UNKNOWN_CATEGORY)) {
                continue; // skip links with unknown category
            }

            // sum the flow between 6am and 10pm
            double totalFlow = countsMap.get(linkId).subList(indexOfStartingCounts, indexOfEndingCounts+1).stream().mapToDouble(Double::doubleValue).sum();

            // only consider links with positive flow
            if (totalFlow>1) {
                // normalize the flow by number of hours and number of lanes to get (veh/h/lane)
                totalFlow = totalFlow / Math.max(1.0, totalNumberOfHours); // normalize by number of hours
                totalFlow = totalFlow / Math.max(network.getLinks().get(linkId).getNumberOfLanes(),1.0); // normalize by number of lanes
                // put the flow in the map
                flowPerCategory.put(linkCategory, flowPerCategory.getOrDefault(linkCategory, 0.0) + totalFlow);
                linksPerCategory.put(linkCategory, linksPerCategory.getOrDefault(linkCategory, 0) + 1);
            }
        }
        // get average flow per link category
        for (int category : flowPerCategory.keySet()) {
            double totalFlow = flowPerCategory.get(category);
            int numLinks = linksPerCategory.get(category);
            if (numLinks == 0) {
                continue;
            }
            double avgFlow = (totalFlow / numLinks);
            flowPerCategory.put(category, avgFlow);
        }
    }

    public void resetCounts(int iteration) {
        flowPerCategory.clear();
        linksPerCategory.clear();
        linkFlowCounter.reset(iteration);
    }

    public double getFlowByCategory(int category) {
        return flowPerCategory.getOrDefault(category, 0.0);
    }

    public double getFlowByCategory(int category, double sampleSize) {
        // this method scale back the flow to 100% sample size
        return getFlowByCategory(category) / sampleSize;
    }

    private void saveCounts(int iteration) {
        String outputFile = outputHierarchy.getIterationFilename(iteration, "flow_by_link_category.csv");

        try (BufferedWriter writer = getBufferedWriter(outputFile)) {
            writer.write("Category;averageFlow(veh/h/lane)\n");
            for (int category : flowPerCategory.keySet()) {
                writer.write(category + ";" + flowPerCategory.get(category) + "\n");
            }
            logger.info("Saved flow by link category to {}", outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Error writing flow by link category to file: " + outputFile);
        }
    }

}
