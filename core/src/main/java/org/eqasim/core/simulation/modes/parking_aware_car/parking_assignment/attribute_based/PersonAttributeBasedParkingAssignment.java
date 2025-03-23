package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based;

import com.google.common.base.Verify;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PersonAttributeBasedParkingAssignment implements ParkingSpaceAssignmentLogic {

    public static final String PARKING_ATTR = "parking";

    private final IdMap<Person, IdMap<Link, ParkingSpace>> parkingPerLinkPerPerson;
    private final IdSet<ParkingType> parkingTypesAvailableForEveryone;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final List<Id<ParkingType>> orderedParkingTypes;

    public PersonAttributeBasedParkingAssignment(List<Id<ParkingType>> orderedParkingTypes, IdSet<ParkingType> parkingTypesAvailableForEveryone, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Population population) {

        parkingPerLinkPerPerson = new IdMap<>(Person.class, population.getPersons().size());
        this.parkingTypesAvailableForEveryone = parkingTypesAvailableForEveryone;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.orderedParkingTypes = orderedParkingTypes;

        ParallelProgress progress = new ParallelProgress("Building population parking cache ...", population.getPersons().size());
        progress.start();

        for(Person person: population.getPersons().values()) {
            progress.update();
            //noinspectionl unchecked
            Map<String, String> linksPerParkingType = (Map<String, String>) person.getAttributes().getAttribute(PARKING_ATTR);
            if(linksPerParkingType == null) {
                continue;
            }
            IdMap<Link, Set<ParkingSpace>> personAlternativesMap = new IdMap<>(Link.class);
            for(Map.Entry<String, String> entry: linksPerParkingType.entrySet()) {
                Id<ParkingType> parkingTypeId = Id.create(entry.getKey(), ParkingType.class);
                for(String linkIdString: entry.getValue().split(",")) {
                    Id<Link> linkId = Id.create(linkIdString, Link.class);
                    ParkingSpace parkingSpace = networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId);
                    Verify.verify(parkingSpace != null, String.format("Parking space of type %s not found on link %s for person %s", entry.getKey(), linkIdString, person.getId().toString()));
                    personAlternativesMap.computeIfAbsent(linkId, id -> new HashSet<>()).add(parkingSpace);
                }
            }

            IdMap<Link, ParkingSpace> personUsageMap = new IdMap<>(Link.class);
            for(Map.Entry<Id<Link>, Set<ParkingSpace>> entry: personAlternativesMap.entrySet()) {
                for(Id<ParkingType> parkingTypeId: orderedParkingTypes) {
                    if(parkingTypesAvailableForEveryone.contains(parkingTypeId)) {
                        ParkingSpace parkingSpace  = networkWideParkingSpaceStore.getLinkParkingSpaces(entry.getKey()).get(parkingTypeId);
                        if(parkingSpace != null) {
                            personUsageMap.put(entry.getKey(), parkingSpace);
                            break;
                        }
                    }
                    ParkingSpace matchingParking = entry.getValue().stream().filter(ps -> ps.parkingType().id().equals(parkingTypeId)).findFirst().orElse(null);
                    if(matchingParking != null) {
                       personUsageMap.put(entry.getKey(), matchingParking);
                       break;
                    }
                }
            }
            parkingPerLinkPerPerson.put(person.getId(), personUsageMap);
        }
        try {
            progress.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Id<Person> personId, Id<Link> linkId) {
        IdMap<Link, ParkingSpace> map = this.parkingPerLinkPerPerson.get(personId);
        ParkingSpace parkingSpace = map != null ? map.get(linkId): null;
        if(parkingSpace == null) {
            for(Id<ParkingType> parkingTypeId: this.orderedParkingTypes) {
                if(!parkingTypesAvailableForEveryone.contains(parkingTypeId)) {
                    continue;
                }
                parkingSpace = this.networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId);
                if(parkingSpace != null) {
                    break;
                }
            }
        }
        return parkingSpace;
    }
}
