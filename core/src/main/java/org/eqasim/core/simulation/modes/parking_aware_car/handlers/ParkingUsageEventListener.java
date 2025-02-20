package org.eqasim.core.simulation.modes.parking_aware_car.handlers;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;

public class ParkingUsageEventListener implements PersonArrivalEventHandler {

    private final String mode;

    public ParkingUsageEventListener(String mode, int aggregationInterval, NetworkWideParkingSpaceStore networkWideParkingSpaceStore, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic) {
        this.mode = mode;
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if(!event.getLegMode().equals(mode)) {
            return;
        }
    }
}