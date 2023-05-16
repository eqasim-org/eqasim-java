package org.eqasim.core.components.headway;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.facilities.Facility;

public interface HeadwayCalculator {
	double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime);

	double calculateHeadway_min(Facility originFacility, Facility destinationFacilty, double departureTime,
			List<Leg> directRoute);
}
