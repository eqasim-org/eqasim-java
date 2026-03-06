package org.eqasim.core.components.traffic_light.delays.webster;

import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.traffic_light.delays.IntersectionGroups;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.TimeBinManager;
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
import java.util.stream.Collectors;

public class WebsterDelay {
    private final Logger logger = LogManager.getLogger(WebsterDelay.class);

    static public String TL_ATTRIBUTE = TrafficLightDelay.TL_ATTRIBUTE;
    private final IdMap<Link, double[]> trafficLightDelays = new IdMap<>(Link.class);
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final WebsterFormula webster;
    private final FlowDataSet flow;
    private final double sampleSize;

//    private final IdMap<Link, String> debuggingMap = new IdMap<>(Link.class);;

    public WebsterDelay(Network network, TimeBinManager timeBinManager,
                        WebsterFormula webster, FlowDataSet flow, double sampleSize) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.webster = webster;
        this.flow = flow;
        this.sampleSize = sampleSize;
    }

    public void initDelays() {
        // Initialize the trafficLightDelays map with empty lists for each link
        logger.info("Initializing traffic light delays");
        for (Link link : network.getLinks().values()) {
            boolean hasTl = (boolean) link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl) {
                trafficLightDelays.put(link.getId(), getZeros(timeBinManager.getNumberOfTlBins()));
            }
        }
    }

    public Double getDelay(Link link, double time) {
        int binIdx = timeBinManager.getTlBinIndex(time);
        double[] linkDelay = trafficLightDelays.get(link.getId());
        if (linkDelay == null) {
            return 0.0; // Default to 0 if no delay is set for this link (e.g., if it does not have a traffic light)
        }
        return linkDelay[binIdx];
    }

    public void clearDelays() {
        // Reset all trafficLightDelays to 0.0
        trafficLightDelays.values().forEach(delayList -> Arrays.fill(delayList, 0.0));
    }

    public void resetDelays() {
        logger.info("Resetting traffic light delays");
        logger.info("The average flow before estimating the delays is {} v/h",flow.getAverageFlow());
        clearDelays();
        buildDelays();
        double averageDelay = trafficLightDelays.values().stream()
                .flatMapToDouble(Arrays::stream)
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
     * We do not adjust the capacity, as the capacity here is not scaled by the sample size.
     */
    public double getFlow(Link link, double time) {
        return Math.min(flow.getFlow(link.getId(), time)/sampleSize,
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
        List<Link> inLinks = node.getInLinks().values().stream()
                .filter(link -> link.getNumberOfLanes() > 0 && link.getCapacity() > 0)
                .collect(Collectors.toList());
        if (inLinks.isEmpty()) return;  // Skip if no valid links

        List<List<Link>> groupedLinks = IntersectionGroups.groupInLinks(inLinks);
        int numGroups = groupedLinks.size();

        Map<Link, Double> flowMap = computeFlowMap(inLinks, time); // Real flow, limited by capacity

        double maxIntersectionCapacity = 1800.0;
        double intersectionCapacity = inLinks.stream().mapToDouble(
                l-> l.getCapacity()/l.getNumberOfLanes()).max().orElse(0.0);
        intersectionCapacity = Math.min(intersectionCapacity, maxIntersectionCapacity);

        double totalY = computeTotalY(flowMap, groupedLinks, intersectionCapacity);
        double cOpt = webster.cOpt(totalY, numGroups); // Optimal cycle time
        double G = webster.gOpt(cOpt, numGroups); // Effective green time

        // assign green time to each group
        Map<Link, Double> gMap = assignGreenTimes(groupedLinks, flowMap, G);

        // correct cOpt based on the assigned green times (happen when min/max green times are hit)
        double newG = groupedLinks.stream()
                                  .map(group -> gMap.get(group.getFirst()))
                                  .mapToDouble(Double::doubleValue)
                                  .sum();
        double newC = newG + webster.L(numGroups);

        // it is obvious that within each group, it might exist some turn conflicts that we cannot solve here
        // this means that we have fewer groups than actual phases, and thus lower delays and losses, and higher green times.
        double correctedC = correctGreenTime(newC, node, numGroups);

        // Compute the delays for each link using Webster's formula
        Map<Link, Double> delayMap = computeDelays(inLinks, flowMap, gMap, correctedC, intersectionCapacity);

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

    private double computeTotalY(Map<Link, Double> flowMap, List<List<Link>> groupedLinks, double intersectionCapacity) {
        // total Yi is the sum of the demands for each phase divided by the capacity of the phase capacity
        double totalYi = 0.0;
        for (List<Link> group : groupedLinks) {
            double groupFlow = 0.0;
            double groupCapacity = 1e-3; // to avoid division by zero
            for (Link link : group) {
                groupFlow += flowMap.get(link);
                groupCapacity += Math.min(link.getCapacity(),
                        intersectionCapacity * link.getNumberOfLanes()); // this is always respected
            }
            totalYi += Math.max(groupFlow / groupCapacity, 5e-2); // make sure all groups contribute something
        }
        return totalYi;
    }

    private double getSumOf(Map<Link, Double> inputMap) {
        return inputMap.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    private Map<Link, Double> assignGreenTimes(List<List<Link>> groupedLinks,
                                               Map<Link, Double> flowMap,
                                               double G) {
        // In this method, we assign green times to each group of links based on their flow / intersection flow ratio

        Map<Link, Double> gMap = new HashMap<>();

        double minGreen = webster.getMinimumGreenTime(); // this is the minimum green time per phase
        double maxGreen = webster.getMaximumGreenTime(); // this is the maximum green time per phase
        double minRatio = minGreen/G;

        int groupCount = groupedLinks.size();
        double greenReserved = minGreen * groupCount; // This is the green time reserved for minimums
        double greenRemaining = G - greenReserved; // This is the green time left to distribute

        // Step 1: Assign minGreen to all
        for (List<Link> group : groupedLinks) {
            for (Link link : group) {
                gMap.put(link, minGreen);
            }
        }

        // If no green time is remaining, return the map with minGreen assigned to each group
        if (greenRemaining <= 0.0) {
            return gMap;
        }

        // Step 2: Compute effective ratios per group (above threshold only)
        List<Double> ratios = new ArrayList<>();
        double minFlow = webster.getMinimumFlowRate();
        double totalIntersectionFlow = Math.max(getSumOf(flowMap),minFlow); // to avoid division by zero
        for (List<Link> group : groupedLinks) {
            double groupRatio = group.stream().mapToDouble(flowMap::get).sum()/totalIntersectionFlow;
            double adjustedRatio = Math.max(1e-3, groupRatio - minRatio); // Adjust the ratio by subtracting the minimum ratio, and ensure it is not negative or zero
            ratios.add(adjustedRatio);
        }

        // Step 3: Distribute remaining green time proportionally
        double totalRatios = ratios.stream().mapToDouble(Double::doubleValue).sum();
        for (int i = 0; i < groupedLinks.size(); i++) {
            double extraGreen = (ratios.get(i) / totalRatios) * greenRemaining;
            List<Link> group = groupedLinks.get(i);
            double groupGreen = minGreen + extraGreen;

            // Cap to maxGreen if needed
            groupGreen = Math.min(groupGreen, maxGreen);

            for (Link link : group) {
                gMap.put(link, groupGreen);
            }
        }

        return gMap;
    }

    private double correctGreenTime(double C, Node node, double numGroups) {
        // here we correct the green times, this might come from the fact that within a group,
        // there are turn conflicts that are not considered when grouping the links.
        List<Link> outLinks = node.getOutLinks().values().stream()
                .filter(l -> l.getAllowedModes().contains("car"))
                .collect(Collectors.toList());

        if (outLinks.isEmpty() | outLinks.size()<=numGroups) {
            return C; // no correction needed
        } else {
            double minGreen = webster.getMinimumGreenTime();
            double additionalGreen = outLinks.size()<=2*numGroups? minGreen:2*minGreen;
            return C + additionalGreen;
        }

    }

    private Map<Link, Double> computeDelays(List<Link> inLinks, Map<Link, Double> flowMap, Map<Link, Double> gMap, double cOpt,
                                            double intersectionCapacity) {
        Map<Link, Double> delayMap = new HashMap<>();
        double minFlow = webster.getMinimumFlowRate(); // Minimum flow to avoid division by zero (one vehicle per hour)
        double maxSat = webster.getMaximumSaturatedRatio(); // Maximum saturation to avoid division by zero in Webster's formula

        for (Link link : inLinks) {
            double g = gMap.get(link);
            double cap = intersectionCapacity * link.getNumberOfLanes();
            double x = Math.min(flowMap.get(link) / cap, maxSat);
            double q = Math.max((flowMap.get(link) / 3600.0), minFlow); // in veh/s
            double delay = webster.delay(cOpt, g, x, q);
            delayMap.put(link, delay);
            // just for debugging purposes
//            if (timeBinManager.getTlBinIndex(time) == 3) {
//                String row = link.getId().toString() + ";" + link.getToNode().getId().toString() +
//                        ";" + String.format("%.3f", cOpt) +
//                        ";" + String.format("%d", numGroups) +
//                        ";" + String.format("%.3f", intersectionCapacity) +
//                        ";" + String.format("%.3f", link.getNumberOfLanes()) +
//                        ";" + String.format("%.3f", g) +
//                        ";" + String.format("%.3f", cap) +
//                        ";" + String.format("%.3f", x) +
//                        ";" + String.format("%.3f", q) +
//                        ";" + String.format("%.3f", delay)+
//                        ";" + String.format("%.3f", Gopt)+
//                        ";" + String.format("%.3f", totalYi)+
//                        ";" + String.format("%.3f", Gold);
//                debuggingMap.put(link.getId(), row);
//            }
        }
        return delayMap;
    }

    private void applyDelays(Map<Link, Double> delayMap, double time) {
        int index = timeBinManager.getTlBinIndex(time);
        for (Map.Entry<Link, Double> entry : delayMap.entrySet()) {
            Id<Link> linkId = entry.getKey().getId();
            trafficLightDelays.computeIfAbsent(linkId, k -> getZeros(timeBinManager.getNumberOfTlBins()));
            double[] delays = trafficLightDelays.get(linkId);
            delays[index] = entry.getValue();
        }
    }

    private double[] getZeros(int size) {
        double[] zeros = new double[size];
        Arrays.fill(zeros, 0.0);
        return zeros;
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
            for (Map.Entry<Id<Link>, double[]> entry : trafficLightDelays.entrySet()) {
                Id<Link> linkId = entry.getKey();
                double[] binFlows = entry.getValue();

                StringBuilder row = new StringBuilder(linkId.toString());
                for (int bin = 0; bin < numberOfBins; bin++) {
                    row.append(String.format(";%.1f", binFlows[bin]));
                }
                writer.write(row.toString() + "\n");
            }
        }
//        // Export the debug list
//        String debugFilename = filename.replace(".csv", "_debug.csv");
//        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(debugFilename))) {
//            // Write header
//            String header = "linkId;nodeId;cOpt;numPhases;intersectionCapacity;numLanes;g;capacity;x;q;delay;Gopt;totalYi;Gold\n";
//            writer.write(header);
//            // Write each link's debug info as a row
//            for (String params : debuggingMap.values()) {
//                writer.write(params + "\n");
//            }
//        }
    }

}
