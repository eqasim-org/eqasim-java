package org.eqasim.core.simulation.modes.parking_aware_car.definitions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class NetworkWideParkingSpaceStore {

    private final IdMap<Link, IdMap<ParkingType, ParkingSpace>> parkingSpacePerTypePerLinkMap;
    private final Network network;

    public NetworkWideParkingSpaceStore(Network network) {
        this.parkingSpacePerTypePerLinkMap = new IdMap<>(Link.class);
        this.network = network;
    }

    public IdMap<ParkingType, ParkingSpace> getLinkParkingSpaces(Id<Link> linkId) {
        return this.parkingSpacePerTypePerLinkMap.get(linkId);
    }
}
