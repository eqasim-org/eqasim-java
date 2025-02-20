package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonAttributeBasedParkingAssignment implements ParkingSpaceAssignmentLogic {

    public static final String PARKING_ATTR = "parking";

    private final IdSet<ParkingType> parkingTypesAvailableForEveryone;
    private final List<Id<ParkingType>> orderedParkingTypes;

    private final Population population;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final IdMap<Person, IdMap<Link, Set<ParkingSpace>>> parkingsPerPersonPerLink;
    private final IdMap<Person, IdMap<Link, ParkingSpace>> parkingPerLinkPerPerson;

    public PersonAttributeBasedParkingAssignment(List<Id<ParkingType>> orderedParkingTypes, IdSet<ParkingType> parkingTypesAvailableForEveryone, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Population population, Network network) {
        this.parkingTypesAvailableForEveryone = parkingTypesAvailableForEveryone;
        this.population = population;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.orderedParkingTypes = orderedParkingTypes;

        this.parkingsPerPersonPerLink = new IdMap<>(Person.class);
        for(Person person: population.getPersons().values()) {
            Map<String, String> linksPerParkingType = (Map<String, String>) person.getAttributes().getAttribute(PARKING_ATTR);
            IdMap<Link, Set<ParkingSpace>> personMap = new IdMap<>(Link.class);
            for(Map.Entry<String, String> entry: linksPerParkingType.entrySet()) {
                Id<ParkingType> parkingTypeId = Id.create(entry.getKey(), ParkingType.class);
                for(String linkIdString: entry.getValue().split(",")) {
                    Id<Link> linkId = Id.create(linkIdString, Link.class);
                    ParkingSpace parkingSpace = networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId);
                    Verify.verify(parkingSpace != null, String.format("Parking space of type %s not found on link %s for person %s", entry.getKey(), linkIdString, person.getId().toString()));
                    personMap.computeIfAbsent(linkId, id -> new HashSet<>()).add(parkingSpace);
                }
            }
            this.parkingsPerPersonPerLink.put(person.getId(), personMap);
        }

        parkingPerLinkPerPerson = new IdMap<>(Person.class);

        for(Person person: population.getPersons().values()) {
            IdMap<Link, ParkingSpace> personMap = new IdMap<>(Link.class);
            for(Id<Link> linkId: network.getLinks().keySet()) {
                for(Id<ParkingType> parkingTypeId: orderedParkingTypes) {
                    if(parkingTypesAvailableForEveryone.contains(parkingTypeId)) {

                    } else {

                    }
                }
            }
        }
    }

    @Override
    public ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Person person, Id<Link> linkId) {
        return null;
    }
}
