package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.eqasim.core.simulation.modes.parking_aware_car.routing.InitialParkingAssignment;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.households.Household;
import org.matsim.households.Households;

import java.util.*;

public class ParkingUsageEventListener implements PersonArrivalEventHandler, PersonDepartureEventHandler, MobsimBeforeCleanupListener, MobsimInitializedListener {

    public record ParkingUsageRecord(Id<Person> personId, ParkingSpace parkingSpace, double enterTime, double exitTime, double occupancy){}

    private final String mode;

    private final IdMap<Person, Tuple<ParkingSpace, Double>> ongoingParkings;

    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
    private final int aggregationInterval;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    // LinkId -> ParkingType -> timeSlotIndex -> Number of users
    private final IdMap<Link, IdMap<ParkingType, Map<Integer, Double>>> parkingUsage;
    private int lastRecordedTimeSlotIndex;

    private final IdMap<Person, List<ParkingUsageRecord>> parkingUsagesPerPerson;
    private final double qsimEndTime;

    private final Population population;
    private final IdMap<Person, Double> parkingOccupancyWeights;

    public ParkingUsageEventListener(String mode, int aggregationInterval, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, double qsimEndTime, Population population, Households households) {
        this.mode = mode;
        this.ongoingParkings = new IdMap<>(Person.class);
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
        this.aggregationInterval = aggregationInterval;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.parkingUsage = new IdMap<>(Link.class);
        this.parkingUsagesPerPerson = new IdMap<>(Person.class);
        this.lastRecordedTimeSlotIndex = -1;
        this.qsimEndTime = qsimEndTime;
        this.population = population;
        this.parkingOccupancyWeights = new IdMap<>(Person.class);
        initializeParkingOccupancyWeights(households, population);
    }

    private void initializeParkingOccupancyWeights(Households households, Population population) {
        for(Household household: households.getHouseholds().values()) {
            int numberOfCars = (int) household.getAttributes().getAttribute("number_of_vehicles");
            IdSet<Person> drivingPersonIds = new IdSet<>(Person.class);
            household.getMemberIds().stream().map(population.getPersons()::get)
                    .filter(Objects::nonNull)
                    .filter(this::drives).map(Person::getId).forEach(drivingPersonIds::add);
            for(Id<Person> personId: household.getMemberIds()) {
                Person person = population.getPersons().get(personId);
                if(person == null) {
                    continue;
                }
                double weight = 0;
                if (numberOfCars > 0 && drivingPersonIds.contains(personId)) {
                    weight = Math.min(1.0, (double) numberOfCars / drivingPersonIds.size());
                }
                this.parkingOccupancyWeights.put(personId, weight);
            }
        }
    }

    public int getTimeSlotIndex(double time) {
        return (int) Math.floor(time/this.aggregationInterval);
    }


    public IdMap<Link, IdMap<ParkingType, Map<Integer, Double>>> getParkingUsage() {
        return parkingUsage;
    }

    public IdMap<Person, List<ParkingUsageRecord>> getParkingUsagesPerPerson() {
        return parkingUsagesPerPerson;
    }

    public int getLastRecordedTimeSlotIndex() {
        return lastRecordedTimeSlotIndex;
    }

    public double getSlotStartTime(int index) {
        return index * aggregationInterval;
    }

    public double getSlotEndTime(int index) {
        return (index + 1) * aggregationInterval;
    }

    private void updateLastRecordedTimeSlotIndex(int timeSlotIndex) {
        if(this.lastRecordedTimeSlotIndex < timeSlotIndex) {
            this.lastRecordedTimeSlotIndex = timeSlotIndex;
        }
    }

    private boolean drives(Person person) {
        if("none".equals(person.getAttributes().getAttribute("carAvailability"))) {
            return false;
        }
        if("no".equals(PersonUtils.getLicense(person))) {
            return false;
        }
        return true;
    }

    @Override
    public void notifyMobsimInitialized(MobsimInitializedEvent e) {
        for(Person person: this.population.getPersons().values()) {
            if(!drives(person)) {
                continue;
            }
            Id<Link> initialVehicleLocation = Id.createLinkId((String)person.getAttributes().getAttribute(InitialParkingAssignment.INITIAL_VEHICLE_LOCATION_ATTRIBUTE));
            ParkingSpace parkingSpace = this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, person.getId(), initialVehicleLocation);
            Verify.verify(parkingSpace != null);
            ongoingParkings.put(person.getId(), Tuple.of(parkingSpace, 0.0));
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(!event.getLegMode().equals(mode)) {
            return;
        }
        ParkingSpace parkingSpace = this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, event.getPersonId(), event.getLinkId());
        Verify.verify(parkingSpace != null);
        ongoingParkings.put(event.getPersonId(), Tuple.of(parkingSpace, event.getTime()));
    }

    public void handleEvent(PersonDepartureEvent event) {
        if(!event.getLegMode().equals(mode)) {
            return;
        }
        Tuple<ParkingSpace, Double> ongoingParking = this.ongoingParkings.remove(event.getPersonId());

        if(ongoingParking == null) {
            ParkingSpace parkingSpace = this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, event.getPersonId(), event.getLinkId());
            Verify.verify(parkingSpace != null);
            ongoingParking = Tuple.of(parkingSpace, 0.0);
        } else {
            Verify.verify(ongoingParking.getFirst().linkId().equals(event.getLinkId()), "Person %s is picking the car from a different link where it was previously parked", event.getPersonId().toString());
        }


        int startingIndex = getTimeSlotIndex(ongoingParking.getSecond());
        int endingIndex = getTimeSlotIndex(event.getTime());

        updateLastRecordedTimeSlotIndex(endingIndex);

        Map<Integer, Double> parkingUsage = this.parkingUsage
                .computeIfAbsent(event.getLinkId(), key -> new IdMap<>(ParkingType.class))
                .computeIfAbsent(ongoingParking.getFirst().parkingType().id(), id -> new HashMap<>());

        double occupancy = this.parkingOccupancyWeights.get(event.getPersonId());

        for(int i=startingIndex; i<=endingIndex; i++) {
            parkingUsage.put(i, parkingUsage.getOrDefault(i, 0.0)+occupancy);
        }

        if(!ongoingParking.getFirst().parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
            this.parkingUsagesPerPerson.computeIfAbsent(event.getPersonId(), key -> new ArrayList<>()).add(new ParkingUsageRecord(event.getPersonId(), ongoingParking.getFirst(), ongoingParking.getSecond(), this.getSlotEndTime(endingIndex), occupancy));
        }
    }

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
        for(Map.Entry<Id<Person>, Tuple<ParkingSpace, Double>> entry : ongoingParkings.entrySet()) {
            Tuple<ParkingSpace, Double> ongoingParking = entry.getValue();
            int startingIndex = getTimeSlotIndex(ongoingParking.getSecond());
            int endingIndex = Math.max(getTimeSlotIndex(qsimEndTime), lastRecordedTimeSlotIndex);

            Map<Integer, Double> parkingUsage = this.parkingUsage
                    .computeIfAbsent(ongoingParking.getFirst().linkId(), key -> new IdMap<>(ParkingType.class))
                    .computeIfAbsent(ongoingParking.getFirst().parkingType().id(), id -> new HashMap<>());

            double occupancy = this.parkingOccupancyWeights.get(entry.getKey());
            for(int i=startingIndex; i<=endingIndex; i++) {
                parkingUsage.put(i, parkingUsage.getOrDefault(i, 0.0)+occupancy);
            }
            if(!ongoingParking.getFirst().parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
                this.parkingUsagesPerPerson.computeIfAbsent(entry.getKey(), key -> new ArrayList<>()).add(new ParkingUsageRecord(entry.getKey(), ongoingParking.getFirst(), ongoingParking.getSecond(), this.getSlotEndTime(endingIndex), occupancy));
            }
        }
    }

    @Override
    public void reset(int iteration) {
        this.ongoingParkings.clear();
        this.parkingUsage.clear();
        this.lastRecordedTimeSlotIndex = -1;
        this.parkingUsagesPerPerson.clear();
    }
}