package org.eqasim.core.simulation.modes.parking_aware_car.mode_choice.penalty;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingType;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public interface ParkingAwareCarPenaltyProvider {

    double getPenalty(Id<Person> person, Id<Link> link, double parkingStartTime, double parkingEndTime, Id<ParkingType> parkingType);

    void update(int iteration);
}
