package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public interface ParkingSpaceAssignmentLogic {

    ParkingSpace getUsedParkingSpace(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Person person, Id<Link> linkId);
}
