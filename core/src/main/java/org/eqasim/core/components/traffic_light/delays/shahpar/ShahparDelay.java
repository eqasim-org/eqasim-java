package org.eqasim.core.components.traffic_light.delays.shahpar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ShahparDelay {
    private final Logger logger = LogManager.getLogger(ShahparDelay.class);
    private double alpha = 3.5; // constant for the formula of the dalay
    private double beta = 4.85; // constant for the formula of the delay
    private double eta = 1.48;  // exponent for the formula of the delay

    private final IdMap<Node, Double> ffMap = new IdMap<>(Node.class);
    private final IdMap<Link, Double> rowMap = new IdMap<>(Link.class);
    private final IdMap<Node, Double> nodesDegrees = new IdMap<>(Node.class);


    private final double sampleSize;
    private final FlowDataSet flow;

    public ShahparDelay(Network network, FlowDataSet flow, double sampleSize) {
        this.sampleSize = sampleSize;
        this.flow = flow;

        for (Node node : network.getNodes().values()) {
            // Step 1: Assign degree to each node
            assignDegreeToNode(node);
            // Step 2: Assign FF to each intersection node
            assignFfToNode(node);
            // Step 3: Assign ROW to each in-link of each intersection node
            assignRowToInLinks(node);
        }

        logger.info("Shahpar delay initialized with {} intersection nodes.", ffMap.size());
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
    public void setBeta(double beta) {
        this.beta = beta;
    }
    public void setEta(double eta) {
        this.eta = eta;
    }

    public double getDelay(Link link, double time) {
        Node intersectionNode = link.getToNode();
        Double degree = nodesDegrees.get(intersectionNode.getId());
        // 1. Check if the intersection node has a degree assigned, and this degree is higher then 2 (otherwise it is a simple connection)
        if (degree == null || degree <= 2) {
            return 0.0;
        }
        // 2. estimate the delay using the Shahpar formula
        double FF = ffMap.get(intersectionNode.getId());
        double ROW = rowMap.get(link.getId());

        // 3. Get the saturation ration of the intersection
        Collection<Link> inLinks = getLinksCar(intersectionNode, "in").values();
        double intersectionCapacity = Collections.max(inLinks.stream().map(Link::getCapacity).toList());
        double intersectionFlow = inLinks.stream().mapToDouble(l -> flow.getFlow(l.getId(), time)).sum();
        double intersectionSaturation = Math.min(intersectionFlow / intersectionCapacity,1.2); // saturation ratio capped at 1.2

        // 4. Calculate the delay using the Shahpar formula
        Double delay = FF*ROW*(alpha+beta*Math.pow(intersectionSaturation, eta));
        return (delay != null && Double.isFinite(delay)) ? delay : 0.0;
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
        // Collect unique neighboring node IDs from in-links and out-links
        if (nodesDegrees.get(intersectionNode.getId()) <= 2) {
            // If only two unique neighboring nodes, it's a simple connection
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
                // U-turns (in-link to same in-link)
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
        if (numTurns < 2 || numTurns > 20) {
            // print all values
            logger.warn("Found {} turns for intersection {} with {} entries and {} exits and {} prohibited turns.",
                        numTurns, intersectionNode.getId(), nEntries, nExits, numProhibited);
        }
        double ffValue = numTurns * Math.min(1.2, (nEntries + 1.0) / (nExits + 1.0));
        ffMap.put(intersectionNode.getId(), ffValue);
    }

    private String formatTurnPair(Id<Link> fromId, Id<Link> toId) {
        return fromId + "->" + toId;
    }

    private void assignRowToInLinks(Node node) {
        List<Link> inLinks = new ArrayList<>(node.getInLinks().values());
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
//    logger.info("✅ Network loaded with " + scenario.getNetwork().getNodes().size() + " nodes and " +
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