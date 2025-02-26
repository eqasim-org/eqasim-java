package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
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

    private final IdMap<Person, IdMap<Link, ParkingSpace>> parkingPerLinkPerPerson;

    public PersonAttributeBasedParkingAssignment(List<Id<ParkingType>> orderedParkingTypes, IdSet<ParkingType> parkingTypesAvailableForEveryone, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Population population, Network network) {

        IdMap<Person, IdMap<Link, Set<ParkingSpace>>> potentialParkingsPerLinkPerPerson = new IdMap<>(Person.class);
        for(Person person: population.getPersons().values()) {
            //noinspection unchecked
            Map<String, String> linksPerParkingType = (Map<String, String>) person.getAttributes().getAttribute(PARKING_ATTR);
            if(linksPerParkingType == null) {
                continue;
            }
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
            potentialParkingsPerLinkPerPerson.put(person.getId(), personMap);
        }

        parkingPerLinkPerPerson = new IdMap<>(Person.class);

        for(Person person: population.getPersons().values()) {
            IdMap<Link, ParkingSpace> personMap = new IdMap<>(Link.class);
            for(Id<Link> linkId: network.getLinks().keySet()) {
                for(Id<ParkingType> parkingTypeId: orderedParkingTypes) {
                    ParkingSpace parkingSpace = networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId);
                    if(parkingSpace == null) {
                        continue;
                    }
                    IdMap<Link, Set<ParkingSpace>> potentialParkingsPerLink = potentialParkingsPerLinkPerPerson.get(person.getId());
                    if(parkingTypesAvailableForEveryone.contains(parkingTypeId) || (potentialParkingsPerLink != null && potentialParkingsPerLink.get(linkId).contains(parkingSpace))) {
                        personMap.put(linkId, parkingSpace);
                        break;
                    }
                }
            }
            parkingPerLinkPerPerson.put(person.getId(), personMap);
        }
    }

    @Override
    public ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Id<Person> personId, Id<Link> linkId) {
        return this.parkingPerLinkPerPerson.get(personId).get(linkId);
    }
}
