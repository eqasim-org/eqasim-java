package org.eqasim.core.components.network_calibration.Processors;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.network_calibration.LinkCategorizer;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.flow.LinkFlowCounter;
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

    private final Map<Integer, Double> flowPerCategory = new HashMap<>();
    private final Map<Integer, Integer> linksPerCategory = new HashMap<>();
    private final Map<Integer, FloatArrayList> errors = new HashMap<>();
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
                continue; // skip links not in the counts processor (consider all when no counts file is provided)
            }

            Integer linkCategory = countsProcessor.getLinkCategory(linkId);
            if ((linkCategory==null) || (linkCategory == LinkCategorizer.UNKNOWN_CATEGORY)) {
                continue; // skip links with unknown category
            }

            // sum the flow of the day
            double totalFlow = linkFlowCounter.getDailyCounts(linkId);

            // only consider links with positive flow
            if (totalFlow>1) {
                // normalize the flow by number of hours and number of lanes to get (veh/h/lane)
                totalFlow = totalFlow / Math.max(1.0, totalNumberOfHours); // normalize by number of hours
                totalFlow = totalFlow / Math.max(network.getLinks().get(linkId).getNumberOfLanes(),1.0); // normalize by number of lanes
                // put the flow in the map
                flowPerCategory.put(linkCategory, flowPerCategory.getOrDefault(linkCategory, 0.0) + totalFlow);
                linksPerCategory.put(linkCategory, linksPerCategory.getOrDefault(linkCategory, 0) + 1);

                double linkCounts = countsProcessor.getLinkCounts(linkId);
                if (linkCounts>0.0) {
                    float error = (float) ((totalFlow / sampleSize - linkCounts) / linkCounts);
                    errors.computeIfAbsent(linkCategory, k -> new FloatArrayList(32)).add(error);
                }
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
        errors.clear();
        // linkFlowCounter.reset(iteration); // no need to call reset, it will be called anyway before mobsim
    }

    public double getFlowByCategory(int category, double inputSampleSize) {
        // this method scale back the flow to 100% sample size
        return flowPerCategory.getOrDefault(category, 0.0) / inputSampleSize;
    }

    public double getFlowByCategory(int category) {
        // this method scale back the flow to 100% sample size
        return flowPerCategory.getOrDefault(category, 0.0) / sampleSize;
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


    private final double C = 0.05;
    private final double EPSILON = 0.03;
    public double getUnbiasedError(int category){
        if (countsProcessor.size()==0) {
            return Double.POSITIVE_INFINITY;
        }

        FloatArrayList catErrors = errors.get(category);
        int nMore = 0;
        int nLess = 0;
        int nTot  = 0;
        for (float e:  catErrors) {
            if (e > EPSILON) {
                nMore ++;
            } else if (e < EPSILON) {
                nLess ++;
            }
            nTot++;
        }

        return nTot==0? 0.0:(nMore - nLess) / (double) nTot;
    }

    public boolean doUpdate(int category) {
        if (countsProcessor.size()==0) {
            return true;
        }
        double unbiasedError = getUnbiasedError(category);
        return Math.abs(unbiasedError) > C;
    }

}
