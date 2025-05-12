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
import org.matsim.contribs.discrete_mode_choice.model.mode_availability.ModeAvailability;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.events.MobsimInitializedEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;
import org.matsim.core.mobsim.framework.listeners.MobsimInitializedListener;
import org.matsim.households.Household;
import org.matsim.households.Households;

import java.util.*;

public class ParkingUsageEventListener implements PersonArrivalEventHandler, PersonDepartureEventHandler, MobsimBeforeCleanupListener, MobsimInitializedListener {

    public static class ParkingUsageRecord {

        private Id<Person> personId;
        private ParkingSpace parkingSpace;
        private double enterTime;
        private double exitTime;
        private double occupancy;
        private double parkingOccupancyOnArrival;
        private double parkingOccupancyOnDeparture;

        private ParkingUsageRecord(Id<Person> personId, ParkingSpace parkingSpace, double enterTime, double occupancy, double parkingOccupancyOnArrival) {
            this(personId, parkingSpace, enterTime, -1, occupancy, parkingOccupancyOnArrival, -1);

        }

        private ParkingUsageRecord(Id<Person> personId, ParkingSpace parkingSpace, double enterTime, double exitTime, double occupancy, double parkingOccupancyOnArrival, double parkingOccupancyOnDeparture) {
            this.personId = personId;
            this.parkingSpace = parkingSpace;
            this.enterTime = enterTime;
            this.exitTime = exitTime;
            this.occupancy = occupancy;
            this.parkingOccupancyOnArrival = parkingOccupancyOnArrival;
            this.parkingOccupancyOnDeparture = parkingOccupancyOnDeparture;
        }

        public Id<Person> getPersonId() {
            return personId;
        }

        public ParkingSpace getParkingSpace() {
            return parkingSpace;
        }

        public double getEnterTime() {
            return enterTime;
        }

        public double getExitTime() {
            return exitTime;
        }

        public double getOccupancy() {
            return occupancy;
        }

        public double getParkingOccupancyOnArrival() {
            return parkingOccupancyOnArrival;
        }

        public double getParkingOccupancyOnDeparture() {
            return parkingOccupancyOnDeparture;
        }
    }

    private final String mode;

    private final IdMap<Person, ParkingUsageRecord> ongoingParkingsPerPerson;
    private final IdMap<Link, IdMap<ParkingType, IdSet<Person>>> currentlyParkedPersons;

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

    private final ModeAvailability modeAvailability;

    public ParkingUsageEventListener(String mode, int aggregationInterval, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, double qsimEndTime, Population population, Households households, ModeAvailability modeAvailability) {
        this.mode = mode;
        this.ongoingParkingsPerPerson = new IdMap<>(Person.class);
        this.currentlyParkedPersons = new IdMap<>(Link.class);
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
        this.aggregationInterval = aggregationInterval;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.parkingUsage = new IdMap<>(Link.class);
        this.parkingUsagesPerPerson = new IdMap<>(Person.class);
        this.lastRecordedTimeSlotIndex = -1;
        this.qsimEndTime = qsimEndTime;
        this.population = population;
        this.parkingOccupancyWeights = new IdMap<>(Person.class);
        this.modeAvailability = modeAvailability;
        initializeParkingOccupancyWeights(households, population);
    }

    private void initializeParkingOccupancyWeights(Households households, Population population) {
        for(Household household: households.getHouseholds().values()) {
            Object attribute = household.getAttributes().getAttribute("number_of_vehicles");
            int numberOfCars = 1;
            if(attribute != null) {
                numberOfCars = (int) attribute;
            }
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
        return this.modeAvailability.getAvailableModes(person, new TripListConverter().convert(person.getSelectedPlan())).contains(this.mode);
    }

    private double getCurrentParkingOccupancy(ParkingSpace parkingSpace) {
        return getCurrentParkingOccupancy(parkingSpace.linkId(), parkingSpace.parkingType().id());
    }

    private double getCurrentParkingOccupancy(Id<Link> linkId, Id<ParkingType> parkingTypeId) {
        return Optional.ofNullable(this.currentlyParkedPersons.get(linkId)).map(m -> m.get(parkingTypeId)).map(l -> l.stream().mapToDouble(parkingOccupancyWeights::get).sum()).orElse(0.0);
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
            ongoingParkingsPerPerson.put(person.getId(), new ParkingUsageRecord(person.getId(), parkingSpace, 0, parkingOccupancyWeights.get(person.getId()), this.getCurrentParkingOccupancy(parkingSpace)));
            currentlyParkedPersons.computeIfAbsent(parkingSpace.linkId(), key -> new IdMap<>(ParkingType.class)).computeIfAbsent(parkingSpace.parkingType().id(), key -> new IdSet<>(Person.class)).add(person.getId());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(!event.getLegMode().equals(mode)) {
            return;
        }
        ParkingSpace parkingSpace = this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, event.getPersonId(), event.getLinkId());
        Verify.verify(parkingSpace != null);
        ongoingParkingsPerPerson.put(event.getPersonId(), new ParkingUsageRecord(event.getPersonId(), parkingSpace, event.getTime(), parkingOccupancyWeights.get(event.getPersonId()), this.getCurrentParkingOccupancy(parkingSpace)));
        currentlyParkedPersons.computeIfAbsent(parkingSpace.linkId(), key -> new IdMap<>(ParkingType.class)).computeIfAbsent(parkingSpace.parkingType().id(), key -> new IdSet<>(Person.class)).add(event.getPersonId());
    }

    public void handleEvent(PersonDepartureEvent event) {
        if(!event.getLegMode().equals(mode)) {
            return;
        }
        ParkingUsageRecord ongoingParking = this.ongoingParkingsPerPerson.remove(event.getPersonId());
        Verify.verify(ongoingParking != null);
        Verify.verify(ongoingParking.parkingSpace.linkId().equals(event.getLinkId()), "Person %s is picking the (at %s) car from a different link (%s) where it was previously parked (%s)", event.getPersonId().toString(), String.valueOf(event.getTime()), event.getLinkId().toString(), ongoingParking.parkingSpace.linkId().toString());


        int startingIndex = getTimeSlotIndex(ongoingParking.enterTime);
        int endingIndex = getTimeSlotIndex(event.getTime());

        updateLastRecordedTimeSlotIndex(endingIndex);

        Map<Integer, Double> parkingUsage = this.parkingUsage
                .computeIfAbsent(event.getLinkId(), key -> new IdMap<>(ParkingType.class))
                .computeIfAbsent(ongoingParking.parkingSpace.parkingType().id(), id -> new HashMap<>());

        double occupancy = this.parkingOccupancyWeights.get(event.getPersonId());

        for(int i=startingIndex; i<=endingIndex; i++) {
            parkingUsage.put(i, parkingUsage.getOrDefault(i, 0.0)+occupancy);
        }

        currentlyParkedPersons.get(event.getLinkId()).get(ongoingParking.parkingSpace.parkingType().id()).remove(event.getPersonId());

        if(!ongoingParking.parkingSpace.parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
            ongoingParking.exitTime = event.getTime();
            ongoingParking.parkingOccupancyOnDeparture = getCurrentParkingOccupancy(ongoingParking.parkingSpace);
            this.parkingUsagesPerPerson.computeIfAbsent(event.getPersonId(), key -> new ArrayList<>()).add(ongoingParking);
        }
    }

    @Override
    public void notifyMobsimBeforeCleanup(MobsimBeforeCleanupEvent e) {
        for(ParkingUsageRecord parkingUsageRecord : ongoingParkingsPerPerson.values()) {
            int startingIndex = getTimeSlotIndex(parkingUsageRecord.enterTime);
            int endingIndex = Math.max(getTimeSlotIndex(qsimEndTime), lastRecordedTimeSlotIndex);

            Map<Integer, Double> parkingUsage = this.parkingUsage
                    .computeIfAbsent(parkingUsageRecord.parkingSpace.linkId(), key -> new IdMap<>(ParkingType.class))
                    .computeIfAbsent(parkingUsageRecord.parkingSpace.parkingType().id(), id -> new HashMap<>());

            double occupancy = parkingUsageRecord.occupancy;
            for(int i=startingIndex; i<=endingIndex; i++) {
                parkingUsage.put(i, parkingUsage.getOrDefault(i, 0.0)+occupancy);
            }

            currentlyParkedPersons.get(parkingUsageRecord.parkingSpace.linkId()).get(parkingUsageRecord.parkingSpace.parkingType().id()).remove(parkingUsageRecord.personId);

            if(!parkingUsageRecord.parkingSpace.parkingType().id().equals(this.networkWideParkingSpaceStore.getFallBackParkingType().id())) {
                parkingUsageRecord.exitTime = qsimEndTime;
                parkingUsageRecord.parkingOccupancyOnDeparture = getCurrentParkingOccupancy(parkingUsageRecord.parkingSpace);
                this.parkingUsagesPerPerson.computeIfAbsent(parkingUsageRecord.personId, key -> new ArrayList<>()).add(parkingUsageRecord);
            }
        }
    }

    @Override
    public void reset(int iteration) {
        this.ongoingParkingsPerPerson.clear();
        this.parkingUsage.clear();
        this.lastRecordedTimeSlotIndex = -1;
        this.parkingUsagesPerPerson.clear();
        this.currentlyParkedPersons.clear();
    }
}