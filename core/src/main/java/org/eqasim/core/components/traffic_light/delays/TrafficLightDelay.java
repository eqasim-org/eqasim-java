package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic_light.TrafficLightConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterFormula;
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

public class TrafficLightDelay {
    private final Logger logger = LogManager.getLogger(TrafficLightDelay.class);

    static public String TL_ATTRIBUTE = "traffic_light";
    private final IdMap<Link, List<Double>> delays = new IdMap<>(Link.class);;
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final WebsterFormula webster;
    private final double sampleSize;
    private int currentIteration;
    private final int tlStartingIteration;

    public TrafficLightDelay(Network network, TimeBinManager timeBinManager,
                             WebsterFormula webster, TrafficLightConfigGroup trafficLightConfigGroup,
                             double sampleSize) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.sampleSize = sampleSize;
        this.webster = webster;
        this.tlStartingIteration = trafficLightConfigGroup.getTlStartingIteration();
        this.currentIteration = 0;

        setTlAttributeToAllLinks();
        initDelays(network);
    }
    private void setTlAttributeToAllLinks() {
        // Set the traffic light attribute to all links in the network
        logger.info("Setting Tl attribute to all links");
        for (Link link : network.getLinks().values()) {
            Object hasTl = link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl==null) {
                link.getAttributes().putAttribute(TL_ATTRIBUTE, false); // Default to false
            } else  {
                boolean hasTlAsBool = Boolean.parseBoolean(hasTl.toString()); // Safer conversion
                link.getAttributes().putAttribute(TL_ATTRIBUTE, hasTlAsBool);
            }
        }
    }

    public void initDelays(Network network) {
        // Initialize the delays map with empty lists for each link
        logger.info("Initializing traffic light delays");
        for (Link link : network.getLinks().values()) {
            boolean hasTl = (boolean) link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl) {
                delays.put(link.getId(), new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
            }
        }
    }

    public double getDelay(Link link, double time) {
        if (!delays.containsKey(link.getId())) {
            return 0.0; // Return 0 if the link is not in the delays' map or time out of bounds
        }
        if (currentIteration<tlStartingIteration) {
            return 0.0; // No delays before the traffic light module starts
        }
        if (time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime()){
            return 0.0;
        }
        int binIdx = timeBinManager.getBinIndex(time);
        Double delayValue = delays.get(link.getId()).get(binIdx);
        return delayValue != null ? delayValue : 0.0; // Default to 0 if no delay is set
    }

    public void clearDelays() {
        // Reset all delays to 0.0
        delays.values().forEach(delayList -> Collections.fill(delayList, 0.0));
    }

    public void resetDelays(int iteration, FlowDataSet flow) {
        logger.info("Resetting traffic light delays");
        clearDelays();
        buildDelays(flow);
        currentIteration = iteration+1; // delays are reset at the end of an iteration, and used in the next iteration
    }

    private void buildDelays(FlowDataSet flow) {
        logger.info("Building traffic light delays");
        Set<Id<Node>> visitedNodes = new HashSet<>();
        for (Link link : network.getLinks().values()) {
            boolean hasTl = (boolean) link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl) {
                Node node = link.getToNode();
                if (!visitedNodes.contains(node.getId())) {
                    for (double time : timeBinManager.getBinsCenters()) {
                        computeNodeDelays(node, flow, time); // This computes the delays within each bin
                    }
                    visitedNodes.add(node.getId());
                }
            }
        }
    }

    private void computeNodeDelays(Node node, FlowDataSet flow, double time) {
        List<Link> inLinks = new ArrayList<>(node.getInLinks().values());
        List<List<Link>> groupedLinks = IntersectionGroups.groupInLinks(inLinks);
        int numGroups = groupedLinks.size();

        Map<Link, Double> flowMap = computeFlowMap(inLinks, flow, time); // Real flow, rescaled by sample size, and limited by capacity
        Map<Link, Double> yiMap = computeYiMap(inLinks, flowMap);
        double totalYi = getSumOf(yiMap);

        double cOpt = webster.cOpt(totalYi, numGroups); // Optimal cycle time
        double G = webster.gOpt(cOpt, numGroups); // Effective green time
        // assign green time to each group
        Map<Link, Double> gMap = assignGreenTimes(groupedLinks, flowMap, G);
        Map<Link, Double> delayMap = computeDelays(inLinks, flowMap, gMap, cOpt, numGroups, time);

        applyDelays(delayMap, time);
    }

    private Map<Link, Double> computeFlowMap(List<Link> inLinks, FlowDataSet flow, double time) {
        Map<Link, Double> flowMap = new HashMap<>();
        for (Link inLink : inLinks) {
            double rawFlow = Math.min(flow.getFlow(inLink.getId(), time), inLink.getCapacity());
            flowMap.put(inLink, rawFlow / sampleSize);
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

    private Map<Link, Double> assignGreenTimes(List<List<Link>> groupedLinks, Map<Link, Double> flowMap, double G) {
        Map<Link, Double> gMap = new HashMap<>();
        double minGreen = webster.getMinimumGreenTime();
        double totalGreen = 0.0;
        double totalFlow = getSumOf(flowMap);
        double totalCapacity = groupedLinks.stream()
            .flatMap(List::stream)
            .mapToDouble(Link::getCapacity)
            .sum();

        for (List<Link> group : groupedLinks) {
            double g; // Default green time for the group
            if (totalFlow>0) {
                double groupFlow = group.stream().mapToDouble(flowMap::get).sum();
                g = Math.max(G * (groupFlow / totalFlow), minGreen);
            } else {
                double groupCapacity = group.stream().mapToDouble(Link::getCapacity).sum();
                g = Math.max(G * (groupCapacity / totalCapacity), minGreen);
            }
            for (Link link : group) {
                gMap.put(link, g);
            }
            totalGreen += g ;
        }
        // Ensure G is respected as the sum of green times
        if (Math.abs(totalGreen-G) > 3.0) { // Allow a tolerance of 3 seconds
            double adjustment = G/ totalGreen; // Scale factor to adjust green times
            gMap.replaceAll((l, v) -> gMap.get(l) * adjustment);
        }
        return gMap;
    }

    private Map<Link, Double> computeDelays(List<Link> inLinks, Map<Link, Double> flowMap, Map<Link, Double> gMap, double cOpt,
                                            int numGroups, double time) {
        Map<Link, Double> delayMap = new HashMap<>();
        double minFlow = webster.getMinimumFlowRate(); // Minimum flow to avoid division by zero (one vehicle per hour)
        double maxSat = webster.getMaximumSaturatedRatio(); // Maximum saturation to avoid division by zero in Webster's formula
        double minGreen = webster.getMinimumGreenTime();
        double minCycle = webster.getMinimumCycleTime(numGroups); // Minimum cycle time based on the number of groups

        double c = Math.max(cOpt, minCycle);
        for (Link link : inLinks) {
            double g = Math.max(gMap.get(link), minGreen);
            double cap = link.getCapacity() / sampleSize; // Capacity rescaled by sample size because flow is also rescaled
            double x = Math.min(flowMap.get(link) / cap, maxSat);
            double q = Math.max(flowMap.get(link) / 3600.0, minFlow);
            double delay = Math.max(webster.delay(c, g, x, q), 0.0);
            if (Double.isNaN(delay) || Double.isInfinite(delay)) {
                logger.warn("   Computed delay is not a valid double for link {} at time {}: {}", link.getId(), time, delay);
                logger.warn("   inputs: c=" + c + ", g=" + g + ", x=" + x + ", q=" + q+"\n");
                delay = 0.0;
            }
            delayMap.put(link, delay);
        }
        return delayMap;
    }

    private void applyDelays(Map<Link, Double> delayMap, double time) {
        int index = timeBinManager.getBinIndex(time);
        for (Map.Entry<Link, Double> entry : delayMap.entrySet()) {
            Id<Link> linkId = entry.getKey().getId();
            delays.computeIfAbsent(linkId, k -> new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
            delays.get(linkId).set(index, entry.getValue());
        }
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
            for (Map.Entry<Id<Link>, List<Double>> entry : delays.entrySet()) {
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
