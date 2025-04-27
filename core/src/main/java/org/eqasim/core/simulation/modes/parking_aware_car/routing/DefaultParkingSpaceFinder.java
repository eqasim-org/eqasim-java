package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.common.util.DistanceUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.Facility;

import java.util.*;
import java.util.stream.IntStream;

public class DefaultParkingSpaceFinder implements ParkingSpaceFinder {

    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;

    private final QuadTree<Link> linksWithParkingsQuadTree;
    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
    private final ParkingUsageEventListener parkingUsageEventListener;
    private final ScenarioExtent scenarioExtent;
    private final Network network;

    private final IdMap<Person, Map<Coord, List<ParkingSpace>>> potentialParkingSpacesPerPerson;

    private final double assumedParkingDuration;
    private final int searchRadius;

    public DefaultParkingSpaceFinder(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Network network, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, ParkingUsageEventListener parkingUsageEventListener, ScenarioExtent scenarioExtent, double assumedParkingDuration, int searchRadius) {
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.network = network;
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

        this.potentialParkingSpacesPerPerson = new IdMap<>(Person.class);

        this.assumedParkingDuration = assumedParkingDuration;
        this.searchRadius = searchRadius;
    }

    private double averageParkingOccupancy(double startTime, ParkingSpace parkingSpace) {
        int beginIndex = this.parkingUsageEventListener.getTimeSlotIndex(startTime);
        int endIndex = this.parkingUsageEventListener.getTimeSlotIndex(startTime + assumedParkingDuration);
        return IntStream.range(beginIndex, endIndex+1)
                .mapToDouble(i -> Optional.ofNullable(this.parkingUsageEventListener.getParkingUsage().get(parkingSpace.linkId()))
                        .map(map -> map.get(parkingSpace.parkingType().id()))
                        .map(map -> map.get(i))
                        .orElse(0.0))
                .average()
                .getAsDouble();
    }


    public ParkingSpace findParkingSpace(Person person, Facility facility, double parkingStartTime) {
        Coord coord = facility.getCoord();

        Optional<ParkingSpace> selectedParkingSpace;

        if(this.scenarioExtent == null || this.scenarioExtent.isInside(coord)) {
            Map<Coord, List<ParkingSpace>> personMap = this.potentialParkingSpacesPerPerson.computeIfAbsent(person.getId(), k -> new HashMap<>());
            List<ParkingSpace> potentialParkingSpaces;
            if(!personMap.containsKey(coord)) {
                Collection<Link> linksWithParking =  this.linksWithParkingsQuadTree.getDisk(coord.getX(), coord.getY(), searchRadius);
                potentialParkingSpaces = linksWithParking.stream().map(l -> this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, person.getId(), l.getId()))
                        .filter(parkingSpace -> parkingSpace != null && !parkingSpace.parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id()))
                        .toList();
                personMap.put(coord, potentialParkingSpaces);
            } else {
                potentialParkingSpaces = personMap.get(coord);
            }
            List<ParkingSpace> filteredParkingSpaces = potentialParkingSpaces.stream().filter(parkingSpace -> this.averageParkingOccupancy(parkingStartTime, parkingSpace) < parkingSpace.capacity()).toList();
            if(!filteredParkingSpaces.isEmpty()) {
                potentialParkingSpaces = filteredParkingSpaces;
            }
            selectedParkingSpace = potentialParkingSpaces.stream().min(Comparator.comparingDouble(parkingSpace -> DistanceUtils.calculateDistance(network.getLinks().get(parkingSpace.linkId()).getCoord(), coord)));
        } else {
            selectedParkingSpace = Optional.empty();
        }

        return selectedParkingSpace.orElseGet(() -> this.parkingSpaceAssignmentLogic.getUsedParkingSpace(networkWideParkingSpaceStore, person.getId(), facility.getLinkId()));
    }

    public ParkingSpaceAssignmentLogic getParkingUsageLogic() {
        return this.parkingSpaceAssignmentLogic;
    }

    public NetworkWideParkingSpaceStore getNetworkWideParkingSpaceStore() {
        return this.networkWideParkingSpaceStore;
    }
}
