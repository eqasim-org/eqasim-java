package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import com.google.common.base.Verify;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;

import java.util.HashMap;
import java.util.Map;

public class ParkingUsageEventListener implements PersonArrivalEventHandler, PersonDepartureEventHandler {

    private final String mode;

    private final IdMap<Person, Tuple<ParkingSpace, Double>> ongoingParkings;

    private final ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic;
    private final int aggregationInterval;
    private final NetworkWideParkingSpaceStore networkWideParkingSpaceStore;
    // LinkId -> ParkingType -> timeSlotIndex -> Number of users
    private final IdMap<Link, IdMap<ParkingType, Map<Integer, Integer>>> parkingUsage;
    private int lastRecordedTimeSlotIndex;

    public ParkingUsageEventListener(String mode, int aggregationInterval, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic) {
        this.mode = mode;
        this.ongoingParkings = new IdMap<>(Person.class);
        this.parkingSpaceAssignmentLogic = parkingSpaceAssignmentLogic;
        this.aggregationInterval = aggregationInterval;
        this.networkWideParkingSpaceStore = networkWideParkingSpaceStore;
        this.parkingUsage = new IdMap<>(Link.class);
        this.lastRecordedTimeSlotIndex = -1;
    }

    private int getTimeSlotIndex(double time) {
        return (int) Math.floor(time/this.aggregationInterval);
    }


    IdMap<Link, IdMap<ParkingType, Map<Integer, Integer>>> getParkingUsage() {
        return parkingUsage;
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
        Tuple<ParkingSpace, Double> ongoingParking = this.ongoingParkings.get(event.getPersonId());
        if(ongoingParking == null) {
            ParkingSpace parkingSpace = this.parkingSpaceAssignmentLogic.getUsedParkingSpace(this.networkWideParkingSpaceStore, event.getPersonId(), event.getLinkId());
            Verify.verify(parkingSpace != null);
            ongoingParking = Tuple.of(parkingSpace, 0.0);
        }

        int startingIndex = getTimeSlotIndex(ongoingParking.getSecond());
        int endingIndex = getTimeSlotIndex(event.getTime());

        updateLastRecordedTimeSlotIndex(endingIndex);

        Map<Integer, Integer> parkingUsage = this.parkingUsage
                .computeIfAbsent(event.getLinkId(), key -> new IdMap<>(ParkingType.class))
                .computeIfAbsent(ongoingParking.getFirst().parkingType().id(), id -> new HashMap<>());

        for(int i=startingIndex; i<=endingIndex; i++) {
            parkingUsage.put(i, parkingUsage.getOrDefault(i, 0)+1);
        }
    }

    @Override
    public void reset(int iteration) {
        this.ongoingParkings.clear();
        this.parkingUsage.clear();
        this.lastRecordedTimeSlotIndex = -1;
    }
}