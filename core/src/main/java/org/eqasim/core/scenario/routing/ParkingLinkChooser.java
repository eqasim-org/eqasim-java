package org.eqasim.core.scenario.routing;

import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.facilities.Facility;
import org.matsim.utils.objectattributes.attributable.AttributesUtils;

import com.google.inject.Inject;

public class ParkingLinkChooser implements MultimodalLinkChooser{

    private final Network parkingNetwork;
    
    @Inject
    public ParkingLinkChooser(Network network, ParkingAvailabilityData parkingAvailabilityData){
        this.parkingNetwork = createParkingNetwork(network, parkingAvailabilityData);
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


    @Override
    public Link decideOnLink(Facility facility, Network network) {

        Link accessActLink = null;
        Id<Link> accessActLinkId = null;

        try {
            accessActLinkId = facility.getLinkId();
        } catch (Exception ee){
            // do nothing
        }

        if (accessActLinkId != null) {
            accessActLink = this.parkingNetwork.getLinks().get(accessActLinkId);
        }

        if (accessActLink != null) {
            return accessActLink;
        }
        else {
            if( facility.getCoord()==null ) {
                throw new RuntimeException("link for facility cannot be determined when neither facility link id nor facility coordinate given") ;
            }
            //accessActLink = network.getLinks().get(NetworkUtils.getNearestLink(parkingNetwork, facility.getCoord()).getId());
            accessActLink = NetworkUtils.getNearestLink(this.parkingNetwork, facility.getCoord());
            Gbl.assertNotNull(accessActLink);
        }

        return accessActLink;
    }
}

