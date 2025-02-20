package org.eqasim.core.simulation.modes.parking_aware_car.definitions;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public record ParkingSpace(ParkingType parkingType, Id<Link> linkId, int capacity) {

}
