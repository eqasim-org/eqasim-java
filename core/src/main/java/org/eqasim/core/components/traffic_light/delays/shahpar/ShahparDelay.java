package org.eqasim.core.components.traffic_light.delays.shahpar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShahparDelay {
    private final Logger logger = LogManager.getLogger(ShahparDelay.class);
    private double alpha; // constant for the formula of the dalay
    private double beta; // constant for the formula of the delay
    private double eta;  // exponent for the formula of the delay
    private final double maximumSaturation; // maximum saturation ratio for the intersection

    private final IdMap<Node, Double> ffMap = new IdMap<>(Node.class);
    private final IdMap<Link, Double> rowMap = new IdMap<>(Link.class);
    private final IdMap<Node, Double> nodesDegrees = new IdMap<>(Node.class);
    private final IdMap<Link, List<Double>> delays = new IdMap<>(Link.class);;
    private boolean mapsAreInitialized;

    private final FlowDataSet flow;
    private final Network network;
    private final TimeBinManager timeBinManager;
    private final double flowRatio; // Ratio to convert flow to hours, based on the time bin size
    private final double sampleSize;

    public ShahparDelay(Network network, FlowDataSet flow, TimeBinManager timeBinManager, ShahparConfigGroup config,
                        double sampleSize) {
        this.alpha = config.getAlpha();
        this.beta = config.getBeta();
        this.eta = config.getEta();
        this.maximumSaturation = config.getMaximumSaturation();
        this.flow = flow;
        this.network = network;
        this.timeBinManager = timeBinManager;
        this.mapsAreInitialized = false;
        this.flowRatio = 3600.0 / timeBinManager.getBinSize();
        this.sampleSize = sampleSize;

        logger.info("Shahpar delay initialized with {} intersection nodes.", ffMap.size());
    }


    public void initDelays(){
        logger.info("Initializing unsignalized intersection's delays");
        if (!mapsAreInitialized) {
            for (Node node : network.getNodes().values()) {
                // Step 1: Assign degree to each node
                assignDegreeToNode(node);
                // Step 2: Assign FF to each intersection node
                assignFfToNode(node);
                // Step 3: Assign ROW to each in-link of each intersection node
                assignRowToInLinks(node);
            }
            mapsAreInitialized = true;
        }
        // limit memory usage by removing the links with 0 delay from memory
        for (Link link : network.getLinks().values()) {
            if (considerLink(link)) {
                    delays.put(link.getId(), new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0)));
                }
            }
    }

    public boolean considerLink(Link link) {
        return (link.getAllowedModes().contains("car") &&
                getNodeDegree(link.getToNode().getId()) > 2);
    }

    public void resetDelays(){
        logger.info("Resetting unsignalized intersection's delays");
        clearDelays();
        buildDelays();
    }

    public void clearDelays() {
        // Reset all delays to 0.0
        delays.values().forEach(delayList -> Collections.fill(delayList, 0.0));
    }

    public void buildDelays(){
        for (Link link : network.getLinks().values()) {
            if (considerLink(link)) {
                List<Double> delayList = delays.get(link.getId());
                if (delayList != null) {
                    double[] binCenters = timeBinManager.getBinsCenters();
                    for (int i = 0; i < timeBinManager.getNumberOfBins(); i++) {
                        double time = binCenters[i];
                        double delay = computeDelay(link, time);
                        delayList.set(i, delay);
                    }
                }
            }
        }
    }

    public Double getDelay(Link link, double time){
        return delays.get(link.getId()).get(timeBinManager.getBinIndex(time));
    }

    public Double getNodeDegree(Id<Node> nodeId) {
        // Returns the degree of the node, or null if not assigned
        return nodesDegrees.get(nodeId);
    }

    public double getFlow(Link link, double time) {
        return Math.min(flow.getFlow(link.getId(), time)*flowRatio/sampleSize,
                        link.getCapacity()); // Adjust flow based on the time bin size, rescale it to 100%, and cap it by the capacity of the link.
    }

    public double computeDelay(Link link, double time) {
        Node intersectionNode = link.getToNode();
        // 1. estimate the FF and ROW of the delay Shahpar formula
        Double FF = ffMap.get(intersectionNode.getId());
        Double ROW = rowMap.get(link.getId());
        if (FF == null || ROW == null) {
            logger.warn("FF or ROW not assigned for intersection node {} or link {}. Returning 0.0 delay.", intersectionNode.getId(), link.getId());
            return 0.0; // Return 0.0 if FF or ROW is not assigned
        }

        // 2. Get the saturation ration of the intersection
        Collection<Link> inLinks = getLinksCar(intersectionNode, "in").values();
        double intersectionCapacity = Collections.max(inLinks.stream().map(Link::getCapacity).toList());
        double intersectionFlow = inLinks.stream().mapToDouble(l -> getFlow(l, time)).sum();
        double intersectionSaturation = Math.min(intersectionFlow / intersectionCapacity, maximumSaturation); // saturation ratio capped at 1.2

        // 3. Calculate the delay using the Shahpar formula
        double delay = FF*ROW*(alpha+beta*Math.pow(intersectionSaturation, eta));
        if (!Double.isFinite(delay)||delay<0.0){
            logger.warn("The computed delay for link {} at time {} is wrong ({}). Returning 0.0", link.getId(), time, delay);
            return 0.0; // Return 0.0 if the delay is not finite or negative
        }
        return Math.min(delay, 40.0); // cap the delay at 40 seconds
    }


    private Map<Id<Link>, Link> getLinksCar(Node node, String direction) {
        // Returns a map of car-allowed in-links for the given node
        if (direction.equalsIgnoreCase("out")) {
            return node.getOutLinks().values().stream()
                    .filter(link -> link.getAllowedModes() != null && link.getAllowedModes().contains("car"))
                    .collect(Collectors.toMap(Link::getId, Function.identity()));
        }
        return node.getInLinks().values().stream()
                .filter(link -> link.getAllowedModes() != null && link.getAllowedModes().contains("car"))
                .collect(Collectors.toMap(Link::getId, Function.identity()));
    }

    public void assignDegreeToNode(Node node) {
        // The degree here is the degree of undirected graph, i.e. the number of neighboring nodes.
        // but this is only for car allowed links, so we need to filter them first
        Map<Id<Link>, Link> inLinksCar = getLinksCar(node, "in");
        Map<Id<Link>, Link> outLinksCar = getLinksCar(node, "out");

        Set<Id<Node>> neighboringNodeIds = new HashSet<>();
        neighboringNodeIds.addAll(inLinksCar.values().stream().map(Link::getFromNode).map(Node::getId).collect(Collectors.toSet()));
        neighboringNodeIds.addAll(outLinksCar.values().stream().map(Link::getToNode).map(Node::getId).collect(Collectors.toSet()));
        nodesDegrees.put(node.getId(), (double) neighboringNodeIds.size());
    }

    public void assignFfToNode(Node intersectionNode) {
        // Step 1: Filter car-allowed in-links and out-links
        Map<Id<Link>, Link> inLinksCar = getLinksCar(intersectionNode, "in");
        Map<Id<Link>, Link> outLinksCar = getLinksCar(intersectionNode, "out");
        int nEntries = inLinksCar.size();
        int nExits = outLinksCar.size();
        int minimumTurns = Math.max(nEntries, nExits); // this should be the minimum number of turns to consider at the intersection

        // Step 2: Check consistencies
        // Check if there are enough car entries and exits to calculate FF
        if ((nEntries <= 1 && nExits <= 1) || nEntries == 0 || nExits == 0) {
            ffMap.put(intersectionNode.getId(), 0.0);
            return;
        }
        // Check if the intersection node has a degree assigned, and this degree is higher than 2 (otherwise it is a simple connection)
        if (nodesDegrees.get(intersectionNode.getId()) <= 2) {
            ffMap.put(intersectionNode.getId(), 0.0);
            return;
        }


        // Step 3: Track prohibited turns (U-turns and disallowed sequences)
        Set<String> prohibitedTurnPairs = new HashSet<>();
        for (Link inLink : inLinksCar.values()) {
            Id<Link> inId = inLink.getId();
            // Disallowed next link sequences (turn restrictions)
            DisallowedNextLinks disallowed = (DisallowedNextLinks) inLink.getAttributes().getAttribute("disallowedNextLinks");
            if (disallowed!=null) {
                List<List<Id<Link>>> sequences = disallowed.getDisallowedLinkSequences("car");
                if (sequences != null) {
                    sequences.stream()
                            .flatMap(List::stream)
                            .filter(outLinksCar::containsKey)
                            .map(outId -> formatTurnPair(inId, outId))
                            .forEach(prohibitedTurnPairs::add);
                }
            }
        }
        // now we make sure thet U turns are removed, but only if the number of turns is above the minimum threshold
        if ((nEntries * nExits - prohibitedTurnPairs.size()) > minimumTurns) {
            for (Link inLink : inLinksCar.values()) {
                Id<Link> inId = inLink.getId();
                Node fromNode = inLink.getFromNode();
                // U-turns
                for (Link outLink : outLinksCar.values()) {
                    // U-turn check
                    if (fromNode.equals(outLink.getToNode())) {
                        prohibitedTurnPairs.add(formatTurnPair(inId, outLink.getId()));
                    }
                }
            }
        }

        // Step 4: Calculate FF value
        int numProhibited = prohibitedTurnPairs.size();
        int numTurns = Math.max((nEntries * nExits - numProhibited), minimumTurns); // ensure at least minimum turns
        double ffValue = numTurns * Math.min(1.2, (nEntries + 1.0) / (nExits + 1.0));

        // step 5: clip ffValue
        ffValue = Math.max(Math.min(ffValue, 10.0), 2.0);
        ffMap.put(intersectionNode.getId(), ffValue);
    }

    private String formatTurnPair(Id<Link> fromId, Id<Link> toId) {
        return fromId + "->" + toId;
    }

    private void assignRowToInLinks(Node node) {
        List<Link> inLinks = new ArrayList<>(getLinksCar(node, "in").values());
        if (inLinks.isEmpty()) {
            return;
        }
        List<Double> capacities = inLinks.stream().map(Link::getCapacity).toList();
        double maxCapacity = Collections.max(capacities);
        double minCapacity = Collections.min(capacities);
        if (maxCapacity == minCapacity) {
            for (Link link : inLinks) {
                rowMap.put(link.getId(), 0.25);
            }
            return;
        }

        for (Link link : inLinks) {
            double capacityRatio = (link.getCapacity() - minCapacity) / (maxCapacity - minCapacity);
            double linkRow = 0.5 * ( 1.0 - capacityRatio); // 0.0 on major street and 0.5 on minor street
            rowMap.put(link.getId(), linkRow);
        }
    }

    public void exportToCSV(String filename) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(filename))) {
            int numberOfBins = timeBinManager.getNumberOfBins();
            // Write header: linkId, bin0, bin1, bin2, ...
            StringBuilder header = new StringBuilder("linkId");
            for (int bin = 0; bin < numberOfBins; bin++) {
                double [] timeIntervals = timeBinManager.getBinInterval(bin);
                double [] timeInHours = {timeIntervals[0]/3600.0, timeIntervals[1]/3600.0};
                header.append(String.format(";bin%d (%.1f-%.1f h)", bin, timeInHours[0], timeInHours[1]));
            }
            writer.write(header.toString() + "\n");

            // Write each link's flows as a row
            for (Map.Entry<Id<Link>, List<Double>> entry : delays.entrySet()) {
                Id<Link> linkId = entry.getKey();
                List<Double> delayValue = entry.getValue();

                StringBuilder row = new StringBuilder(linkId.toString());
                for (int bin = 0; bin < numberOfBins; bin++) {
                    row.append(String.format(";%.1f", delayValue.get(bin)));
                }
                writer.write(row.toString() + "\n");
            }
        }
    }


    // these might be useful for calibration within the simulation
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
    public void setBeta(double beta) {
        this.beta = beta;
    }
    public void setEta(double eta) {
        this.eta = eta;
    }
}

//public static void main(String[] args) {
//    // Path to your MATSim network file (can be relative or absolute)
//    String networkFilePath = "C:/Users/abdel/Desktop/ETH/codes/scenario0p1/switzerland_network.xml.gz";
//
//    // Load empty config and create scenario
//    Config config = ConfigUtils.createConfig();
//    Scenario scenario = ScenarioUtils.createScenario(config);
//
//    // Read the network into the scenario
//    long start = System.nanoTime();
//    new MatsimNetworkReader(scenario.getNetwork()).readFile(networkFilePath);
//    long end = System.nanoTime();
//    logger.info("Reading network took: " + (end - start)/1_000_000_000.0 + " s");
//    // Simple confirmation output
//    logger.info("Network loaded with " + scenario.getNetwork().getNodes().size() + " nodes and " +
//            scenario.getNetwork().getLinks().size() + " links.");
//
//    Network network = scenario.getNetwork();
//
//    start = System.nanoTime();
//    for (Node node : network.getNodes().values()) {
//        assignFfToNode(node);
//        assignRowToInLinks(node);
//    }
//    end = System.nanoTime();
//    logger.info("Computing FF and ROWs took: " + (end - start)/1_000_000_000.0 + " s");
//
//}