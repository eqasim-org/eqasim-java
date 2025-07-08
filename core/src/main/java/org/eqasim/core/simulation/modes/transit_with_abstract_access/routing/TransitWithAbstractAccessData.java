package org.eqasim.core.simulation.modes.transit_with_abstract_access.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;
import org.matsim.pt.transitSchedule.api.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class TransitWithAbstractAccessData {

    private final QuadTree<TransitStopFacility> quadTree;
    private final Network network;

    public TransitWithAbstractAccessData(TransitSchedule transitSchedule, Network network) {

        double[] bounds = NetworkUtils.getBoundingBox(transitSchedule.getFacilities().values().stream().map(Facility::getLinkId).map(network.getLinks()::get).flatMap(link -> Stream.of(link.getFromNode(), link.getToNode())).toList());
        this.quadTree = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);
        Set<Id<TransitStopFacility>> processedFacilities = new HashSet<>();
        for (TransitLine transitLine : transitSchedule.getTransitLines().values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {
                    TransitStopFacility transitStopFacility = transitRouteStop.getStopFacility();
                    if (!processedFacilities.contains(transitStopFacility.getId())) {
                        processedFacilities.add(transitStopFacility.getId());
                        this.quadTree.put(transitStopFacility.getCoord().getX(), transitStopFacility.getCoord().getY(), transitStopFacility);
                    }
                }
            }
        }

        this.network = NetworkUtils.createNetwork();

        // The provided network is cleaned to keep only the biggest cluster
        // This is done to be able to compute paths to PT links from non-PT links.
        // The formers are very often not connected to the car network
        new TransportModeNetworkFilter(network).filter(this.network, Collections.singleton("car"));
    }

    public QuadTree<TransitStopFacility> getQuadTree() {
        return this.quadTree;
    }

    public Network getNetwork() {
        return this.network;
    }

}
