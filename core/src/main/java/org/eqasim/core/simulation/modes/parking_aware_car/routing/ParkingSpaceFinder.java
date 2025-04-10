package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

public interface ParkingSpaceFinder {

    ParkingSpace findParkingSpace(Person person, Facility facility, double parkingStartTime);
}
