package org.eqasim.core.simulation.modes.parking_aware_car.definitions;

import org.matsim.api.core.v01.Id;

public record ParkingType(Id<ParkingType> id) {

    public ParkingType(String id) {
        this(Id.create(id, ParkingType.class));
    }
}
