package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.attribute_based;

import com.google.common.base.Verify;
import org.eqasim.core.misc.ParallelProgress;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;

import java.util.*;

public class EfficientPersonAttributeBasedParkingAssignment implements ParkingSpaceAssignmentLogic {

    public static final String PARKING_ATTR = "parking";

    private final IdSet<ParkingType> parkingTypesAvailableForEveryone;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    private final List<Id<ParkingType>> orderedParkingTypes;

    private final Map<Id<Link>, Integer>[] linkIdIndices;
    private final Map<Id<Person>, Integer> personIndices;
    private final ParkingSpace[][] matrix;


    public EfficientPersonAttributeBasedParkingAssignment(List<Id<ParkingType>> orderedParkingTypes, IdSet<ParkingType> parkingTypesAvailableForEveryone, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Population population) {

        Map<Id<Person>, Map<Id<Link>, ParkingSpace>> parkingPerLinkPerPerson = new HashMap<>(population.getPersons().size());
        this.parkingTypesAvailableForEveryone = parkingTypesAvailableForEveryone;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.orderedParkingTypes = orderedParkingTypes;

        ParallelProgress progress = new ParallelProgress("Building population parking cache ...", population.getPersons().size());
        progress.start();

        System.gc();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Before Memory");
        long beforeMemory = Runtime.getRuntime().totalMemory() -Runtime.getRuntime().freeMemory();

        for(Person person: population.getPersons().values()) {
            progress.update();

            // In the map below, we store the list of parkings that the person might use at each link
            Map<Id<Link>, Set<ParkingSpace>> personAlternativesMap = new HashMap<>();

            // We start by the parkings mentioned in the person attributes
            Map<String, String> linksPerParkingType = (Map<String, String>) person.getAttributes().getAttribute(PARKING_ATTR);
            if(linksPerParkingType == null) {
                continue;
            }
            for(Map.Entry<String, String> entry: linksPerParkingType.entrySet()) {
                Id<ParkingType> parkingTypeId = Id.create(entry.getKey(), ParkingType.class);
                for(String linkIdString: entry.getValue().split(",")) {
                    Id<Link> linkId = Id.create(linkIdString, Link.class);
                    ParkingSpace parkingSpace = networkWideParkingSpaceStore.getLinkParkingSpaces(linkId).get(parkingTypeId);
                    Verify.verify(parkingSpace != null, String.format("Parking space of type %s not found on link %s for person %s", entry.getKey(), linkIdString, person.getId().toString()));
                    personAlternativesMap.computeIfAbsent(linkId, id -> new HashSet<>()).add(parkingSpace);
                }
            }

            Map<Id<Link>, ParkingSpace> personUsageMap = new HashMap<>();
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
            if(!personUsageMap.isEmpty()) {
                parkingPerLinkPerPerson.put(person.getId(), personUsageMap);
            }
        }
        try {
            progress.close();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        personIndices = new HashMap<>(parkingPerLinkPerPerson.size());
        List<Id<Person>> personIdList = new ArrayList<>();
        List<Map<Id<Link>, Integer>> personLinkIdList = new ArrayList<>();
        Map<Id<Person>, Id<Link>[]> personLinkIds = new HashMap<>();

        int currentPersonIndex = 0;
        for(Map.Entry<Id<Person>, Map<Id<Link>, ParkingSpace>> entry: parkingPerLinkPerPerson.entrySet()) {
            personIndices.put(entry.getKey(), currentPersonIndex);
            personIdList.add(entry.getKey());
            currentPersonIndex++;

            int currentLinkIndex = 0;
            List<Id<Link>> linkIdList = new ArrayList<>();

            Map<Id<Link>, Integer> currentPersonLinkIndices = new HashMap<>();
            for(Map.Entry<Id<Link>, ParkingSpace> personLinkEntry: entry.getValue().entrySet()) {
                currentPersonLinkIndices.put(personLinkEntry.getKey(), currentLinkIndex);
                linkIdList.add(personLinkEntry.getKey());
                currentLinkIndex++;
            }

            personLinkIdList.add(currentPersonLinkIndices);
            personLinkIds.put(entry.getKey(), linkIdList.toArray(new Id[linkIdList.size()]));
        }

        linkIdIndices = personLinkIdList.toArray(new Map[personLinkIdList.size()]);

        matrix = new ParkingSpace[personIndices.size()][personLinkIdList.stream().mapToInt(m -> m.size()).max().getAsInt()];

        for(int personIndex = 0; personIndex < personIdList.size(); personIndex++) {
            Id<Person> personId = personIdList.get(personIndex);
            Map<Id<Link>, ParkingSpace> personMap = parkingPerLinkPerPerson.get(personId);
            Id<Link>[] linkIds = personLinkIds.get(personId);
            for(int linkIndex = 0; linkIndex < linkIds.length; linkIndex++) {
                Id<Link> linkId = linkIds[linkIndex];
                matrix[personIndex][linkIndex] = personMap.get(linkId);
            }
        }

        personLinkIds.clear();
        personIdList.clear();
        personLinkIdList.clear();

        System.gc();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Memory consumption in Mb");
        long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((afterMemory - beforeMemory) / (1024 * 1024));
    }

    @Override
    public ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Id<Person> personId, Id<Link> linkId) {
        ParkingSpace parkingSpace = null;
        Integer personIndex = this.personIndices.get(personId);
        if(personIndex != null) {
            Integer linkIndex = linkIdIndices[personIndex].get(linkId);
            if(linkIndex != null) {
                parkingSpace = matrix[personIndex][linkIndex];
            }
        }

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
        if(parkingSpace == null) {
            return new ParkingSpace(this.networkWideParkingSpaceStore.getFallBackParkingType(), linkId, -1);
        }
        return parkingSpace;
    }
}
