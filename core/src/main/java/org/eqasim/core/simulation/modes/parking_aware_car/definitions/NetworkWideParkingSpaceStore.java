package org.eqasim.core.simulation.modes.parking_aware_car.definitions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.Map;

public class NetworkWideParkingSpaceStore {

    public static final String PARKINGS_ATT_PREFIX = "parking:";

    private final IdMap<Link, IdMap<ParkingType, ParkingSpace>> parkingSpacePerTypePerLinkMap;
    private final IdMap<ParkingType, ParkingType> parkingTypes;
    private final Network network;

    public NetworkWideParkingSpaceStore(Network network) {
        this.parkingSpacePerTypePerLinkMap = new IdMap<>(Link.class);
        this.parkingTypes = new IdMap<>(ParkingType.class);
        this.network = network;
        for(Link link: network.getLinks().values()) {
            for(Map.Entry<String, Object> entry: link.getAttributes().getAsMap().entrySet().stream().filter(entry -> entry.getKey().startsWith(PARKINGS_ATT_PREFIX)).toList()) {
                Id<ParkingType> parkingTypeId = Id.create(entry.getKey().substring(PARKINGS_ATT_PREFIX.length()), ParkingType.class);
                ParkingType parkingType = parkingTypes.computeIfAbsent(parkingTypeId, ParkingType::new);
                int capacity = (int) entry.getValue();
                this.parkingSpacePerTypePerLinkMap.computeIfAbsent(link.getId(), linkId -> new IdMap<>(ParkingType.class)).put(parkingTypeId, new ParkingSpace(parkingType, link.getId(), capacity));
            }
        }
    }


    public IdMap<ParkingType, ParkingSpace> getLinkParkingSpaces(Id<Link> linkId) {
        IdMap<ParkingType, ParkingSpace> result = this.parkingSpacePerTypePerLinkMap.get(linkId);
        if(result == null) {
            result = new IdMap<>(ParkingType.class);
        }
        return result;
    }
}
