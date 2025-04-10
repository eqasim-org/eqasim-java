package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.RoutingRequest;

public class ParkingAwareMultimodalLinkChooser implements MultimodalLinkChooser {


    private final ParkingSpaceFinder parkingSpaceFinder;

    public ParkingAwareMultimodalLinkChooser(ParkingSpaceFinder parkingSpaceFinder) {
        this.parkingSpaceFinder = parkingSpaceFinder;
    }

    public static final String LAST_CAR_LOCATION_ATTRIBUTE_NAME = "lastCarLocation";

    @Override
    public Link decideAccessLink(RoutingRequest request, Network network) {
        Id<Link> linkId = (Id<Link>) request.getPerson().getAttributes().getAttribute(LAST_CAR_LOCATION_ATTRIBUTE_NAME);
        if(linkId == null) {
            linkId = (Id<Link>) request.getPerson().getAttributes().getAttribute(InitialParkingAssignment.INITIAL_VEHICLE_LOCATION_ATTRIBUTE);
            if(linkId == null) {
                throw new IllegalStateException(String.format("Initial vehicle location not set for person %s", request.getPerson().getId().toString()));
            }
        }
        return network.getLinks().get(linkId);
    }

    @Override
    public Link decideEgressLink(RoutingRequest request, Network network) {
        ParkingSpace parkingSpace = this.parkingSpaceFinder.findParkingSpace(request.getPerson(), request.getToFacility(), request.getDepartureTime());

        return network.getLinks().get(parkingSpace.linkId());
    }
}
