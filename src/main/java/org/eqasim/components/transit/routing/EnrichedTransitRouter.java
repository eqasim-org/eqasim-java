package org.eqasim.components.transit.routing;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;

public interface EnrichedTransitRouter {
	List<Leg> calculateRoute(final Facility fromFacility, final Facility toFacility, final double departureTime,
			final Person person);
}
