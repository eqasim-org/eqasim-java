package org.eqasim.ile_de_france.policies;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import com.google.inject.Inject;

/**
 * Handles parking assignment for agents
 * 
 * @author akramelb
 */
public class ParkingAssignment {

    private static final Logger log = LogManager.getLogger(NetworkUtils.class);

    Network parkingNetwork;
    Network network;
    ParkingAvailabilityData parkingAvailabilityData;

    @Inject
    public ParkingAssignment(Network network, ParkingAvailabilityData parkingAvailabilityData) {
        this.parkingNetwork = createParkingNetwork(network, parkingAvailabilityData);
        this.parkingAvailabilityData = parkingAvailabilityData;
        this.network = network;
    }


    public Network createParkingNetwork(Network network, ParkingAvailabilityData parkingAvailabilityData){
        Network carNetwork = NetworkUtils.createNetwork();
        new TransportModeNetworkFilter(network).filter(carNetwork, Set.of("car"));
		new NetworkCleaner().run(carNetwork);

        Network parkingNetwork = NetworkUtils.createNetwork();
        NetworkFactory factory = parkingNetwork.getFactory();

        for (Node node : carNetwork.getNodes().values()) {
            Node newNode = factory.createNode(node.getId(), node.getCoord());
            AttributesUtils.copyAttributesFromTo(node, newNode);
            parkingNetwork.addNode(newNode);

        }

        IdSet<Node> nodesToInclude = new IdSet<>(Node.class);
        for (Link link : carNetwork.getLinks().values()) {
            if (parkingAvailabilityData.getParkingAvailability(link.getId())) {
                Id<Node> fromId = link.getFromNode().getId();
				Id<Node> toId = link.getToNode().getId();
				Node fromNode2 = parkingNetwork.getNodes().get(fromId);
				Node toNode2 = parkingNetwork.getNodes().get(toId);
				nodesToInclude.add(fromId);
				nodesToInclude.add(toId);

				Link link2 = factory.createLink(link.getId(), fromNode2, toNode2);
				link2.setAllowedModes(link.getAllowedModes());
				link2.setCapacity(link.getCapacity());
				link2.setFreespeed(link.getFreespeed());
				link2.setLength(link.getLength());
				link2.setNumberOfLanes(link.getNumberOfLanes());
				NetworkUtils.setType(link2, NetworkUtils.getType(link));
				AttributesUtils.copyAttributesFromTo(link, link2);
				parkingNetwork.addLink(link2);
            }
        }

        IdSet<Node> nodesToRemove = new IdSet<>(Node.class);
		for (Node node : carNetwork.getNodes().values()) {
			if (!nodesToInclude.contains(node.getId())) nodesToRemove.add(node.getId());
		}
		for (Id<Node> nodeId : nodesToRemove) parkingNetwork.removeNode(nodeId);

        return parkingNetwork;
    }

    public Link getNearestParkingLink(Coord coordinate){
        return NetworkUtils.getNearestLink(this.parkingNetwork, coordinate);
    }

    public Link getNearestParkingLinkforPerson(Person person, Coord coordinate){

        List<Link> nearestLinks = getNearestLinks(coordinate, 20);
        int index = Math.abs(person.getId().hashCode()) % nearestLinks.size();
        return nearestLinks.get(index);
        
    }

    public List<Link> getNearestLinks(Coord coordinate, int numLinks){
        Set<Node> visitedNodes = new HashSet<>();
        Set<Link> nearestLinks = new LinkedHashSet<>();
        Queue<Link> linkQueue = new LinkedList<>();

        Node nearestNode = NetworkUtils.getNearestNode(parkingNetwork, coordinate);
        visitedNodes.add(nearestNode);

        if (nearestNode == null){
            log.warn("No nearest node found for coordinate " + coordinate);
        }

        else if (nearestNode.getInLinks().isEmpty() && nearestNode.getOutLinks().isEmpty()){
            log.warn("Found nearest node that has no incident links. Will probably crash eventually. Maybe run NetworkCleaner?");
        }

        // Get all incident links from the nearest node
        linkQueue.addAll(NetworkUtils.getIncidentLinks(nearestNode).values()); 
        //nearestLinks.addAll(NetworkUtils.getIncidentLinks(nearestNode).values());

        // Get all incident links from the nearest links
        while (!linkQueue.isEmpty() && nearestLinks.size() < numLinks) {
            Link link = linkQueue.poll();
        
            Node fromNode = link.getFromNode();
            if (!visitedNodes.contains(fromNode)){
                visitedNodes.add(fromNode);
                linkQueue.addAll(NetworkUtils.getIncidentLinks(fromNode).values());
            }
        
            Node toNode = link.getToNode();
            if (!visitedNodes.contains(toNode)){
                visitedNodes.add(toNode);
                linkQueue.addAll(NetworkUtils.getIncidentLinks(toNode).values());
            }
        
            // Add the link to nearestLinks after processing its incident links
            if (parkingNetwork.getLinks().containsKey(link.getId())){
                nearestLinks.add(link);
            }
        }
        if (nearestLinks.size() >= numLinks) {
            return new ArrayList<>(nearestLinks).subList(0, numLinks - 1);
        } else {
            return new ArrayList<>(nearestLinks);
        }

    }

    public List<Link> getNearestLinksinParkingNetwork(Coord coordinate, int numLinks){
        Set<Node> visitedNodes = new HashSet<>();
        Set<Link> nearestLinks = new LinkedHashSet<>();
        Queue<Link> linkQueue = new LinkedList<>();

        Node nearestNode = NetworkUtils.getNearestNode(parkingNetwork, coordinate);
        visitedNodes.add(nearestNode);

        if (nearestNode == null){
            log.warn("No nearest node found for coordinate " + coordinate);
        }

        else if (nearestNode.getInLinks().isEmpty() && nearestNode.getOutLinks().isEmpty()){
            log.warn("Found nearest node that has no incident links. Will probably crash eventually. Maybe run NetworkCleaner?");
        }

        // Get all incident links from the nearest node
        linkQueue.addAll(NetworkUtils.getIncidentLinks(nearestNode).values()); 
        //nearestLinks.addAll(NetworkUtils.getIncidentLinks(nearestNode).values());

        // Get all incident links from the nearest links
        while (!linkQueue.isEmpty() && nearestLinks.size() < numLinks) {
            Link link = linkQueue.poll();
        
            Node fromNode = link.getFromNode();
            if (!visitedNodes.contains(fromNode)){
                visitedNodes.add(fromNode);
                linkQueue.addAll(NetworkUtils.getIncidentLinks(fromNode).values());
            }
        
            Node toNode = link.getToNode();
            if (!visitedNodes.contains(toNode)){
                visitedNodes.add(toNode);
                linkQueue.addAll(NetworkUtils.getIncidentLinks(toNode).values());
            }
        
            // Add the link to nearestLinks after processing its incident links
            nearestLinks.add(link);
        }

        if (nearestLinks.size() >= numLinks) {
            return new ArrayList<>(nearestLinks).subList(0, numLinks - 1);
        } else {
            return new ArrayList<>(nearestLinks);
        }

    }
    

    
}
