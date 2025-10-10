package org.eqasim.core.components.network_calibration.capacities_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.BufferedWriter;
import java.util.*;

import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

public class FlowByLinkCategory {

    private final static Logger logger = LogManager.getLogger(FlowByLinkCategory.class);
    private final Network network;
    private final TrafficCounter counter;
    private final OutputDirectoryHierarchy outputHierarchy;

    private final Map<Integer, Double> counts = new HashMap<>();
    private final Map<Integer, Integer> linkPerCategory = new HashMap<>();
    private final int indexOf6am;
    private final int indexOf10pm;
    private final double totalNumberOfHours;

    public FlowByLinkCategory(Network network, TrafficCounter counter, TimeBinManager timeBinManager,
                              OutputDirectoryHierarchy outputHierarchy) {
        this.network = network;
        this.counter = counter;
        this.outputHierarchy = outputHierarchy;

        this.indexOf6am = timeBinManager.getBinIndex(6 * 3600);
        this.indexOf10pm = timeBinManager.getBinIndex(22 * 3600);

        this.totalNumberOfHours = (timeBinManager.getBinInterval(indexOf10pm)[1] - timeBinManager.getBinInterval(indexOf6am)[0])/3600.0;
    }


    public void updateAndSaveCounts(IterationEndsEvent iterationEndsEvent) {
        updateCounts();
        saveCounts(iterationEndsEvent.getIteration());
    }

    private void updateCounts() {
        // get the counts from the traffic counter
        IdMap<Link, List<Double>> countsMap = counter.getCounts();
        // aggregate the counts by link category
        for (Id<Link> linkId : countsMap.keySet()) {
            Link link = network.getLinks().get(linkId);
            int linkCategory = Utils.getCategory(link);
            if (linkCategory == Utils.unknownCategory) {
                continue; // skip links with unknown category
            }

            // sum the flow between 6am and 10pm
            double totalFlow = countsMap.get(linkId).subList(indexOf6am, indexOf10pm + 1).stream().mapToDouble(Double::doubleValue).sum();

            // only consider links with positive flow
            if (totalFlow>1) {
                // normalize the flow by number of hours and number of lanes to get (veh/h/lane)
                totalFlow = totalFlow / totalNumberOfHours; // normalize by number of hours
                totalFlow = totalFlow / link.getNumberOfLanes(); // normalize by number of lanes
                // put the flow in the map
                counts.put(linkCategory, counts.getOrDefault(linkCategory, 0.0) + totalFlow);
                linkPerCategory.put(linkCategory, linkPerCategory.getOrDefault(linkCategory, 0) + 1);
            }
        }
        // get average flow per link category
        for (int category : counts.keySet()) {
            double totalFlow = counts.get(category);
            int numLinks = linkPerCategory.get(category);
            double avgFlow = (totalFlow / numLinks);
            counts.put(category, avgFlow);
        }
    }

    public void resetCounts(int iteration) {
        counts.clear();
        linkPerCategory.clear();
        counter.reset(iteration);
    }

    public double getFlowByCategory(int category) {
        return counts.getOrDefault(category, 0.0);
    }

    public double getFlowByCategory(int category, double sampleSize) {
        // this method scale back the flow to 100% sample size
        return getFlowByCategory(category) / sampleSize;
    }

    private void saveCounts(int iteration) {
        String outputFile = outputHierarchy.getIterationFilename(iteration, "flow_by_link_category.csv");

        try (BufferedWriter writer = getBufferedWriter(outputFile)) {
            writer.write("Category;averageFlow(veh/h/lane)\n");
            for (int category : counts.keySet()) {
                writer.write(category + ";" + counts.get(category) + "\n");
            }
            logger.info("Saved flow by link category to {}", outputFile);
        } catch (Exception e) {
            throw new RuntimeException("Error writing flow by link category to file: " + outputFile);
        }
    }

}
