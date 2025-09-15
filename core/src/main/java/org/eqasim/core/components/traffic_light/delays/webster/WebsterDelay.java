package org.eqasim.core.components.traffic_light.delays.webster;

import org.eqasim.core.components.traffic_light.delays.IntersectionGroups;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class WebsterDelay {
    private final Logger logger = LogManager.getLogger(WebsterDelay.class);

    static public String TL_ATTRIBUTE = TrafficLightDelay.TL_ATTRIBUTE;
    private final IdMap<Link, List<Double>> trafficLightDelays = new IdMap<>(Link.class);;
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final WebsterFormula webster;
    private final FlowDataSet flow;
    private final double flowRatio; // Ratio to convert flow to hours, based on the time bin size
    private final double sampleSize;

    public WebsterDelay(Network network, TimeBinManager timeBinManager,
                        WebsterFormula webster, FlowDataSet flow, double sampleSize) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.webster = webster;
        this.flow = flow;
        this.flowRatio = 3600.0 / timeBinManager.getBinSize();
        this.sampleSize = sampleSize;
    }

    public void initDelays() {
        // Initialize the trafficLightDelays map with empty lists for each link
        logger.info("Initializing traffic light delays");
        for (Link link : network.getLinks().values()) {
            boolean hasTl = (boolean) link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl) {
                trafficLightDelays.put(link.getId(), new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfTlBins(), 0.0)));
            }
        }
    }

    public Double getDelay(Link link, double time) {
        int binIdx = timeBinManager.getTlBinIndex(time);
        return trafficLightDelays.get(link.getId()).get(binIdx);
    }

    public void clearDelays() {
        // Reset all trafficLightDelays to 0.0
        trafficLightDelays.values().forEach(delayList -> Collections.fill(delayList, 0.0));
    }

    public void resetDelays() {
        logger.info("Resetting traffic light delays");
        logger.info("The average flow before estimating the delays is {}",flow.getAverageFlow()*flowRatio);
        clearDelays();
        buildDelays();
        double averageDelay = trafficLightDelays.values().stream()
                .flatMap(List::stream)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        logger.info("Average traffic light delay after reset: {}", averageDelay);
    }

    /**
     * The time interval would impact the way the formula is used. If a default time interval is set to 3600 seconds (1 hour),
     * the Webster formula will compute delays based on the average flow over that hour and the capacity. However, if the time interval is smaller,
     * the capacity needs to be adjusted accordingly, as the Webster formula assumes a certain flow rate over a cycle time. The 1800 capacity
     * of the intersection that was assumed will also need to be adjusted. Therefore, instead of that, we adjust only the flow because it is
     * the only parameter that is dependent on the time interval. It is scaled to 1 hour so that the formula can be applied correctly without any changes.
     * and to 100% sample size.
     */
    public double getFlow(Link link, double time) {
        return Math.min(flow.getFlow(link.getId(), time)*flowRatio/sampleSize,
                link.getCapacity()); // Adjust flow based on the time bin size, rescale it to 100%, and cap it by the capacity of the link.
    }


    /**
     * Builds the traffic light trafficLightDelays based on the provided flow data set.
     * This method iterates through all links in the network, checks if they have a traffic light,
     * and computes trafficLightDelays for each node with a traffic light.
     */
    private void buildDelays() {
        logger.info("Building traffic light delays using Webster's formula");
        Set<Id<Node>> visitedNodes = new HashSet<>();
        for (Link link : network.getLinks().values()) {
            boolean hasTl = (boolean) link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl) {
                Node node = link.getToNode();
                if (!visitedNodes.contains(node.getId())) {
                    for (double time : timeBinManager.getTlBinsCenters()) {
                        computeNodeDelays(node, time); // This computes the trafficLightDelays within each bin
                    }
                    visitedNodes.add(node.getId());
                }
            }
        }
    }

    // All the methods below are private and used to compute the trafficLightDelays for a given node based on the Webster formula
    // when a node is provided, all its incoming links are considered, and the trafficLightDelays are computed and injected
    // in the trafficLightDelays map, which is indexed by link Id and time bin index.

    private void computeNodeDelays(Node node, double time) {
        List<Link> inLinks = new ArrayList<>(node.getInLinks().values());
        List<List<Link>> groupedLinks = IntersectionGroups.groupInLinks(inLinks);
        int numGroups = groupedLinks.size();

        Map<Link, Double> flowMap = computeFlowMap(inLinks, time); // Real flow, limited by capacity
        Map<Link, Double> yiMap = computeYiMap(inLinks, flowMap);

        double maxIntersectionCapacity = 1800.0;
        double intersectionCapacity = inLinks.stream().mapToDouble(
                l-> l.getCapacity()/l.getNumberOfLanes()).max().orElse(0.0);
        intersectionCapacity = Math.min(intersectionCapacity, maxIntersectionCapacity);

        double totalYi = getSumOf(yiMap)/intersectionCapacity;

        double cOpt = webster.cOpt(totalYi, numGroups); // Optimal cycle time
        double G = webster.gOpt(cOpt, numGroups); // Effective green time
        // assign green time to each group
        Map<Link, Double> gMap = assignGreenTimes(groupedLinks, yiMap, G);
        // correct cOpt based on the assigned green times
        double newG = groupedLinks.stream()
                                  .map(group -> gMap.get(group.getFirst()))
                                  .mapToDouble(Double::doubleValue)
                                  .sum();
        double newC = newG + webster.L(numGroups);

        // Compute the delays for each link using Webster's formula
        Map<Link, Double> delayMap = computeDelays(inLinks, flowMap, gMap, newC, numGroups, intersectionCapacity, time);
        // Apply the computed delays to the trafficLightDelays map
        applyDelays(delayMap, time);
    }

    private Map<Link, Double> computeFlowMap(List<Link> inLinks, double time) {
        Map<Link, Double> flowMap = new HashMap<>();
        for (Link inLink : inLinks) {
            double rawFlow = getFlow(inLink, time);
            flowMap.put(inLink, rawFlow);
        }
        return flowMap;
    }

    private Map<Link, Double> computeYiMap(List<Link> inLinks, Map<Link, Double> flowMap) {
        Map<Link, Double> yiMap = new HashMap<>();
        for (Link inLink : inLinks) {
            double yi = flowMap.get(inLink) / inLink.getNumberOfLanes();
            yiMap.put(inLink, yi);
        }
        return yiMap;
    }

    private double getSumOf(Map<Link, Double> inputMap) {
        return inputMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private Map<Link, Double> assignGreenTimes(List<List<Link>> groupedLinks,
                                               Map<Link, Double> yiMap,
                                               double G) {
        Map<Link, Double> gMap = new HashMap<>();

        double minGreen = webster.getMinimumGreenTime();
        double maxGreen = webster.getMaximumGreenTime();
        double minRatio = minGreen/G; // Set this to your desired threshold

        int groupCount = groupedLinks.size();
        double greenReserved = minGreen * groupCount;
        double greenRemaining = G - greenReserved;

        if (greenRemaining <= 0.0) {
            // Not enough total green time — fallback to equal minimums
            for (List<Link> group : groupedLinks) {
                for (Link link : group) {
                    gMap.put(link, minGreen);
                }
            }
            return gMap;
        }

        // Step 1: Assign minGreen to all
        for (List<Link> group : groupedLinks) {
            for (Link link : group) {
                gMap.put(link, minGreen);
            }
        }

        // Step 2: Compute effective ratios per group (above threshold only)
        List<Double> ratios = new ArrayList<>();
        double totalY = getSumOf(yiMap);
        for (List<Link> group : groupedLinks) {
            double groupRatio = group.stream().mapToDouble(yiMap::get).sum()/totalY;
            double adjustedRatio = Math.max(0.0, groupRatio - minRatio);
            ratios.add(adjustedRatio);
        }

        double totalRatios = ratios.stream().mapToDouble(Double::doubleValue).sum();

        if (totalRatios == 0.0) {
            // No group has enough pressure to gain extra time — all stay at minGreen
            return gMap;
        }

        // Step 3: Distribute remaining green time proportionally
        for (int i = 0; i < groupedLinks.size(); i++) {
            double extraGreen = (ratios.get(i) / totalRatios) * greenRemaining;
            List<Link> group = groupedLinks.get(i);
            double groupGreen = minGreen + extraGreen;

            // Clamp to maxGreen if needed
            groupGreen = Math.min(groupGreen, maxGreen);

            for (Link link : group) {
                gMap.put(link, groupGreen);
            }
        }

        return gMap;
    }


    private Map<Link, Double> computeDelays(List<Link> inLinks, Map<Link, Double> flowMap, Map<Link, Double> gMap, double cOpt,
                                            int numGroups, double intersectionCapacity, double time) {
        Map<Link, Double> delayMap = new HashMap<>();
        double minFlow = webster.getMinimumFlowRate(); // Minimum flow to avoid division by zero (one vehicle per hour)
        double maxSat = webster.getMaximumSaturatedRatio(); // Maximum saturation to avoid division by zero in Webster's formula

        for (Link link : inLinks) {
            double g = gMap.get(link);
            double cap = intersectionCapacity * link.getNumberOfLanes();//link.getCapacity();
            double x = Math.min(flowMap.get(link) / cap, maxSat);
            double q = Math.max((flowMap.get(link) / 3600.0), minFlow); // scale flow to 100% sample
            double delay = webster.delay(cOpt, g, x, q);
            if (Double.isNaN(delay) || Double.isInfinite(delay)) {
                logger.warn("   Computed delay is not a valid double for link {} at time {}: {}", link.getId(), time, delay);
                logger.warn("   inputs: c=" + cOpt + ", g=" + g + ", x=" + x + ", q=" + q+"\n");
                delay = 0.0;
            }
            if (delay>60.0){
                logger.warn("   Computed delay is greater than 60 seconds for link {} at time {}: {}", link.getId(), time, delay);
                logger.warn("   inputs: c=" + cOpt + ", g=" + g + ", x=" + x + ", q=" + q+ ", phases="+numGroups+"\n");
            }
            if (delay<1.0){
                logger.warn("   Computed delay is less than 1 second for link {} at time {}: {}", link.getId(), time, delay);
                logger.warn("   inputs: c=" + cOpt + ", g=" + g + ", x=" + x + ", q=" + q+ ", phases="+numGroups+"\n");
            }
            delayMap.put(link, delay);
        }
        return delayMap;
    }

    private void applyDelays(Map<Link, Double> delayMap, double time) {
        int index = timeBinManager.getTlBinIndex(time);
        for (Map.Entry<Link, Double> entry : delayMap.entrySet()) {
            Id<Link> linkId = entry.getKey().getId();
            trafficLightDelays.computeIfAbsent(linkId, k -> new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfTlBins(), 0.0)));
            trafficLightDelays.get(linkId).set(index, entry.getValue());
        }
    }


    /**
     * Exports the traffic light trafficLightDelays to a CSV file.
     * The CSV will have a header with link IDs and bins, followed by rows of trafficLightDelays for each link.
     *
     * @param filename The name of the file to export the trafficLightDelays to.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
    public void exportToCSV(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            int numberOfBins = timeBinManager.getNumberOfTlBins();
            // Write header: linkId, bin0, bin1, bin2, ...
            StringBuilder header = new StringBuilder("linkId");
            for (int bin = 0; bin < numberOfBins; bin++) {
                double [] timeIntervals = timeBinManager.getTlBinInterval(bin);
                double [] timeInHours = {timeIntervals[0]/3600.0, timeIntervals[1]/3600.0};
                header.append(String.format(";bin%d (%.1f-%.1f h)", bin, timeInHours[0], timeInHours[1]));
            }
            writer.write(header.toString() + "\n");

            // Write each link's flows as a row
            for (Map.Entry<Id<Link>, List<Double>> entry : trafficLightDelays.entrySet()) {
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
