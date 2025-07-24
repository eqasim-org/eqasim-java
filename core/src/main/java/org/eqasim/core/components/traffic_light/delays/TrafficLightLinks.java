package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TimeBinManager;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TrafficLightLinks {
    static public String TL_ATTRIBUTE = "traffic_light";
    private final IdMap<Link, List<Double>> delays = new IdMap<>(Link.class);;
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final double sampleSize;

    public TrafficLightLinks(Network network, FlowDataSet flow, TimeBinManager timeBinManager, double sampleSize) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.sampleSize = sampleSize;
        buildDelays(flow);
    }

    public double getDelay(Link link, double time) {
        if (!delays.containsKey(link.getId())) {
            return 0.0; // Return 0 if the link is not in the delays map
        }
        List<Double> delay = delays.get(link.getId());
        int binIdx = timeBinManager.getBinIndex(time);
        Double delayValue = delay.get(binIdx);
        return delayValue != null ? delayValue : 0.0; // Default to 0 if no delay is set
    }

    public void cleanDelays() {
        // Reset all delays to 0.0
        for (Id<Link> linkId : delays.keySet()) {
            List<Double> delayList = delays.get(linkId);
            delayList.replaceAll(ignored -> 0.0);
        }
    }

    public void resetDelays(FlowDataSet flow) {
        delays.clear();
        buildDelays(flow);
    }

    private void buildDelays(FlowDataSet flow) {
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

        // Get flow and the ratio yi/li for each link
        IdMap<Link, Double> flowMap = new IdMap<>(Link.class);
        IdMap<Link, Double> yiMap = new IdMap<>(Link.class);
        double y = 0.0;
        double totalFlow = 0.0;
        for (Link inLink : inLinks) {
            double flowValue = Math.min(flow.getFlow(inLink.getId(), time), inLink.getCapacity()); // limit the flow by the capacity
            flowValue = flowValue/sampleSize; // rescale the flow to the original value

            double yi = flowValue / inLink.getNumberOfLanes();
            y += yi;
            totalFlow += flowValue;
            flowMap.put(inLink.getId(), flowValue);
            yiMap.put(inLink.getId(), yi);
        }

        // Optimal cycle time
        double cOpt = WebsterFormula.cOpt(y, numGroups);
        // Effective green time
        double G = WebsterFormula.gOpt(cOpt, numGroups);
        // assign green time to each group
        IdMap<Link, Double> gMap = new IdMap<>(Link.class);
        for (List<Link> group : groupedLinks) {
            double groupFlow = group.stream()
                    .mapToDouble(flowMap::get)
                    .sum();
            double ratio = groupFlow / totalFlow; // ratio of flow to total flow
            double g = Math.max(G * ratio, WebsterFormula.getMinimumGreenTime()); // effective green time for the group
            for (Link inLink : group) {
                gMap.put(inLink.getId(), g);
            }
        }
        // Calculate delay for the link using Webster's formula
        IdMap<Link, Double> delayMap = new IdMap<>(Link.class);
        double minimumFlow = 1.0/3600; // small value to avoid division by zero (one vehicle per hour)
        double maximumSaturation = 0.95; // maximum saturation to avoid division by zero in Webster's formula
        double minimumGreenTime = WebsterFormula.getMinimumGreenTime();
        double minimumCycleTime = minimumGreenTime*numGroups + WebsterFormula.L(groupedLinks.size());

        for (Link inLink : inLinks) {
            double c = Math.max(cOpt, minimumCycleTime); // cycle time
            double g = Math.max(gMap.get(inLink.getId()), minimumGreenTime); // effective green time for the link
            double x = Math.min(flowMap.get(inLink.getId())/(inLink.getCapacity()/sampleSize),maximumSaturation) ;// degree of saturation
            double q = Math.max(flowMap.get(inLink.getId())/3600.0, minimumFlow); // in vehicles per second
            double delay = Math.max(WebsterFormula.delay(c, g, x, q), 0.0);
            delayMap.put(inLink.getId(), delay);
        }

        // set the values in the map
        int index = timeBinManager.getBinIndex(time);

        for (Map.Entry<Id<Link>, Double> entry : delayMap.entrySet()) {
            Id<Link> linkId = entry.getKey();
            Double delayValue = entry.getValue();
            if (!delays.containsKey(linkId)) {
                List<Double> delayList = new ArrayList<>(Collections.nCopies(timeBinManager.getNumberOfBins(), 0.0));
                delays.put(linkId, delayList);
            }
            List<Double> linkDelay = delays.get(linkId);
            linkDelay.set(index, delayValue);
        }

    }


}
