package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;

import java.util.*;
import java.util.stream.IntStream;

public class ParkingAwareMultimodalLinkChooser implements MultimodalLinkChooser {

    public static final double HORIZON = 3600 * 3;

    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

    private final QuadTree<Link> linksWithParkingsQuadTree;
    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
    private final ParkingUsageEventListener parkingUsageEventListener;
    private final ScenarioExtent scenarioExtent;

    public ParkingAwareMultimodalLinkChooser(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Network network, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, ParkingUsageEventListener parkingUsageEventListener, ScenarioExtent scenarioExtent) {
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
        this.linksWithParkingsQuadTree = new QuadTree<>(boundingBox[0], boundingBox[1], boundingBox[2], boundingBox[3]);
        for(Link link: network.getLinks().values()) {
            IdMap<ParkingType, ParkingSpace> linkParkingSpaces = this.networkWideParkingSpaceStore.getLinkParkingSpaces(link.getId());
            if(linkParkingSpaces.isEmpty()) {
                continue;
            }
            this.linksWithParkingsQuadTree.put(link.getCoord().getX(), link.getCoord().getY(), link);
        }
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
        this.parkingUsageEventListener = parkingUsageEventListener;
        this.scenarioExtent = scenarioExtent;
    }

    public static final String LAST_CAR_LOCATION_ATTRIBUTE_NAME = "lastCarLocation";

    @Override
    public Link decideAccessLink(RoutingRequest request, Network network) {
        Id<Link> linkId = (Id<Link>) request.getPerson().getAttributes().getAttribute(LAST_CAR_LOCATION_ATTRIBUTE_NAME);
        if(linkId != null) {
            return network.getLinks().get(linkId);
        }
        return network.getLinks().get(request.getFromFacility().getLinkId());
    }

    private double averageParkingOccupancy(double startTime, ParkingSpace parkingSpace) {
        int beginIndex = this.parkingUsageEventListener.getTimeSlotIndex(startTime);
        int endIndex = this.parkingUsageEventListener.getTimeSlotIndex(startTime + HORIZON);
        return IntStream.range(beginIndex, endIndex+1)
                .map(i -> Optional.ofNullable(this.parkingUsageEventListener.getParkingUsage().get(parkingSpace.linkId())).map(map -> map.get(parkingSpace.parkingType().id())).map(map -> map.get(i)).orElse(0))
                .average()
                .getAsDouble();
    }

    @Override
    public Link decideEgressLink(RoutingRequest request, Network network) {
        Coord coord = request.getToFacility().getCoord();
        Collection<Link> linksWithParking =  this.linksWithParkingsQuadTree.getDisk(coord.getX(), coord.getY(), 500);

        Optional<ParkingSpace> selectedParkingSpace;

        if(this.scenarioExtent == null || this.scenarioExtent.isInside(request.getToFacility().getCoord())) {
            selectedParkingSpace = linksWithParking.stream().map(l -> this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, request.getPerson().getId(), l.getId()))
                    .filter(parkingSpace -> parkingSpace != null && !parkingSpace.parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id()))
                    .filter(parkingSpace -> this.averageParkingOccupancy(request.getDepartureTime(), parkingSpace) < parkingSpace.capacity())
                    .min(Comparator.comparingDouble(parkingSpace -> DistanceUtils.calculateDistance(network.getLinks().get(parkingSpace.linkId()).getCoord(), coord)));
        } else {
            selectedParkingSpace = Optional.empty();
        }

        if(selectedParkingSpace.isPresent()) {
            return network.getLinks().get(selectedParkingSpace.get().linkId());
        } else {
            return network.getLinks().get(request.getToFacility().getLinkId());
        }
    }
}
