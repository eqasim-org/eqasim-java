package org.eqasim.examples.zurich_parking.analysis.parking;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.facilities.ActivityFacility;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class ParkingSearchItem {
    // trip attributes
    public Id<Person> personId;
    public int tripId;
    public String tripPurpose;
    public Coord destinationCoord;
    public OptionalTime arrivalTime = OptionalTime.undefined();

    // parking search times
    public OptionalTime parkingSearchStartTime = OptionalTime.undefined();
    public OptionalTime parkingSearchEndTime = OptionalTime.undefined();

    // parking facility attributes
    public Optional<Id<ActivityFacility>> parkingFacilityId;
    public Optional<String> parkingFacilityType;
    public Optional<Coord> parkingCoord;

    // parking search attributes
    public double parkingSearchTime = 0.0;
    public double parkingSearchDistance = 0.0;
    public double egressWalkTime = 0.0;
    public double egressWalkDistance = 0.0;
    public Set<Id<Link>> searchedLinkIds = new HashSet<>();
    public double duplicatedDistance = 0.0;
}
