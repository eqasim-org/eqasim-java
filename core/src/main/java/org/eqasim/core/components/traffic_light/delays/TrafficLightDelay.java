package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic_light.TrafficLightConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterFormula;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.matsim.vehicles.Vehicle;
import org.matsim.core.utils.geometry.CoordUtils;

public class TrafficLightDelay {
    private final Logger logger = LogManager.getLogger(TrafficLightDelay.class);

    static public String TL_ATTRIBUTE = "traffic_light";
    private final IdMap<Link, List<Double>> delays = new IdMap<>(Link.class);;
    private final IdMap<Vehicle, Coord> lastTrafficLightLocation = new IdMap<Vehicle, Coord>(Vehicle.class);
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final WebsterFormula webster;
    private final double sampleSize;
    private int currentIteration;
    private final int tlStartingIteration;

    private final double tlDistanceThreshold = 30.0; // Distance threshold between two tl, to set a delay at the tl intersection

    private int ignoredTlCount = 0; // Counter for ignored traffic lights
    // flags
    public static final double NO_TL = -1; // No traffic light
    public static final double OUT_OF_BOUNDS = -2; // Time out of bounds
    public static final double BEFORE_TL = -3; // Before the traffic light module starts
    public static final double INCORRECT_DELAY = -4; // After the traffic light module ends
    public static final double NO_DELAY = -5; // No delay set (vehicle got a delay in the last 'threshold' meters)

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

    public double getDelay(Link link, double time, Id<Vehicle> vehicleId) {
        if (!delays.containsKey(link.getId())) {
            return NO_TL; // The link does not have a traffic light (or no delays set)
        }
        if (currentIteration<tlStartingIteration) {
            return BEFORE_TL; // No delays before the traffic light module starts
        }
        if (time < timeBinManager.getStartTime() || time > timeBinManager.getEndTime()){
            return OUT_OF_BOUNDS; // Time is out of bounds of the time bins (normal crossing penalty will be applied)
        }

        // at this point, we know that the link has a traffic light and the time is within bounds
        if (!shouldApplyTrafficLightDelay(link, vehicleId)) {
            return NO_DELAY; // No delay if the vehicle has not traveled far enough since the last traffic light
        }
        int binIdx = timeBinManager.getBinIndex(time);
        Double delayValue = delays.get(link.getId()).get(binIdx);
        return delayValue != null ? delayValue : INCORRECT_DELAY; // Default to 0 if no delay is set
    }

    private boolean shouldApplyTrafficLightDelay(Link link, Id<Vehicle> vehicleId) {
        // link should have a traffic light
        Coord lastTrafficLightPosition = lastTrafficLightLocation.get(vehicleId);
        Coord currentCoord = link.getToNode().getCoord();

        // Calculate distance from last traffic light position
        double distanceSinceLastTrafficLight = (lastTrafficLightPosition == null)
                ? 0.0 // If no previous traffic light position, treat as 0 distance
                : CoordUtils.calcEuclideanDistance(lastTrafficLightPosition, currentCoord);

        // if distance higher than the threshold, we should apply the traffic light delay, and update the last known traffic light position
        if (distanceSinceLastTrafficLight > tlDistanceThreshold){
            // Update the last known traffic light position to current link's end
            lastTrafficLightLocation.put(vehicleId, currentCoord);
            return true;
        }else {
            ignoredTlCount += 1; // Increment the counter for ignored traffic lights
            if (ignoredTlCount % 100 == 0) {
                logger.warn("Ignored {} traffic lights so far", ignoredTlCount);
            }
            return false;
        }
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

    /**
     * Builds the traffic light delays based on the provided flow data set.
     * This method iterates through all links in the network, checks if they have a traffic light,
     * and computes delays for each node with a traffic light.
     *
     * @param flow The flow data set containing traffic flow information.
     */
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

    // All the methods below are private and used to compute the delays for a given node based on the Webster formula
    // when a node is provided, all its incoming links are considered, and the delays are computed and injected
    // in the delays map, which is indexed by link Id and time bin index.

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


    /**
     * Exports the traffic light delays to a CSV file.
     * The CSV will have a header with link IDs and bins, followed by rows of delays for each link.
     *
     * @param filename The name of the file to export the delays to.
     * @throws IOException If an I/O error occurs while writing to the file.
     */
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
