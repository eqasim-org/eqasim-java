package org.eqasim.core.components.network_calibration.Processors;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.network_calibration.cost_calibration.PenaltyGroupKey;
import org.matsim.api.core.v01.Id;
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

    private final Map<PenaltyGroupKey, Double> flowPerGroup = new HashMap<>();
    private final Map<PenaltyGroupKey, Integer> linksPerGroup = new HashMap<>();
    private final Map<PenaltyGroupKey, FloatArrayList> errors = new HashMap<>();
    private final double totalNumberOfHours;
    private final double sampleSize;

    public FlowProcessor(Network network, LinkFlowCounter linkFlowCounter, FlowBinManager flowBinManager,
                         CountsProcessor countsProcessor, OutputDirectoryHierarchy outputHierarchy,
                         double sampleSize) {
        this.network = network;
        this.linkFlowCounter = linkFlowCounter;
        this.outputHierarchy = outputHierarchy;
        this.countsProcessor = countsProcessor;
        this.sampleSize = sampleSize;
        this.totalNumberOfHours = flowBinManager.getTotalTime_h();
    }

    public void updateAndSaveCounts(IterationEndsEvent iterationEndsEvent) {
        update();
        saveCounts(iterationEndsEvent.getIteration());
    }

    private void update() {
        // aggregate the counts by link category
        for (Id<Link> linkId : network.getLinks().keySet()) {
            if (!countsProcessor.contains(linkId)) {
                continue; // skip links not in the counts processor
            }

            PenaltyGroupKey groupKey = countsProcessor.getLinkGroup(linkId);
            if (groupKey == null) {
                continue; // skip links with unknown category
            }

            // sum the flow of the day (normalized, by hour, by lane)
            double totalFlow = getTotalLinkFlow(linkId);

            // only consider links with positive flow
            if (totalFlow>0.0) {
                // put the flow in the map
                flowPerGroup.put(groupKey, flowPerGroup.getOrDefault(groupKey, 0.0) + totalFlow);
                linksPerGroup.put(groupKey, linksPerGroup.getOrDefault(groupKey, 0) + 1);

                double linkCounts = countsProcessor.getLinkCounts(linkId);
                if (linkCounts>0.0) {
                    float error = (float) ((totalFlow / sampleSize - linkCounts) / linkCounts);
                    errors.computeIfAbsent(groupKey, k -> new FloatArrayList(32)).add(error);
                }
            }
        }

        // get average flow per link group
        for (PenaltyGroupKey key : flowPerGroup.keySet()) {
            double totalFlow = flowPerGroup.get(key);
            int numLinks = linksPerGroup.get(key);
            if (numLinks == 0) {
                continue;
            }
            double avgFlow = (totalFlow / numLinks);
            flowPerGroup.put(key, avgFlow);
        }
    }

    public double getTotalLinkFlow(Id<Link> linkId) {
        double totalFlow = linkFlowCounter.getDailyCounts(linkId);
        if (totalFlow>1) {
            totalFlow = totalFlow / Math.max(1.0, totalNumberOfHours); // normalize by number of hours
            totalFlow = totalFlow / Math.max(network.getLinks().get(linkId).getNumberOfLanes(),1.0); // normalize by number of lanes
            return totalFlow;
        }
        return 0.0;
    }

    public void resetCounts(int iteration) {
        flowPerGroup.clear();
        linksPerGroup.clear();
        errors.clear();
        // linkFlowCounter.reset(iteration); // no need to call reset, it will be called anyway before mobsim
    }

    public double getFlowByGroup(PenaltyGroupKey key, double inputSampleSize) {
        // this method scale back the flow to 100% sample size
        return flowPerGroup.getOrDefault(key, 0.0) / inputSampleSize;
    }

    public double getFlowByGroup(PenaltyGroupKey key) {
        // this method scale back the flow to 100% sample size
        return flowPerGroup.getOrDefault(key, 0.0) / sampleSize;
    }

    private void saveCounts(int iteration) {
        String outputFile = outputHierarchy.getIterationFilename(iteration, "flow_by_link_category.csv");

        try (BufferedWriter writer = getBufferedWriter(outputFile)) {
            writer.write("linkCategory;isUrban;specialRegion;averageFlow(veh/h/lane)\n");
            for (PenaltyGroupKey key : flowPerGroup.keySet()) {
                writer.write(key.getLinkCategory() + ";" + key.isUrban() + ";" + key.getSpecialRegion() + ";" + flowPerGroup.get(key) + "\n");
            }
            logger.info("Saved flow by link category to {}", outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Error writing flow by link category to file: " + outputFile);
        }
    }


    private final double C = 0.02;
    private final double EPSILON = 0.02;
    public double getUnbiasedError(PenaltyGroupKey key){
        if (countsProcessor.size()==0) {
            return Double.POSITIVE_INFINITY;
        }

        FloatArrayList catErrors = errors.get(key);
        if (catErrors == null || catErrors.isEmpty()) {
            return 0.0;
        }
        int nMore = 0;
        int nLess = 0;
        int nTot  = 0;
        for (float e:  catErrors) {
            if (e > EPSILON) {
                nMore ++;
            } else if (e < -EPSILON) {
                nLess ++;
            }
            nTot++;
        }

        return nTot==0? 0.0:(nMore - nLess) / (double) nTot;
    }

    public boolean doUpdate(PenaltyGroupKey key) {
        if (countsProcessor.size()==0) {
            return true;
        }
        double unbiasedError = getUnbiasedError(key);
        return Math.abs(unbiasedError) > C;
    }

}
