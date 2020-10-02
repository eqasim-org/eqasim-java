package org.eqasim.projects.astra16.travel_time;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class AdjustedTravelTime implements TravelTime {
	private final TravelTime delegate;

	@Inject
	public AdjustedTravelTime(@Named("car") TravelTimeCalculator calculator) {
		this.delegate = calculator.getLinkTravelTimes();
	}

	@Override
	public double getLinkTravelTime(Link link, double time, Person person, Vehicle vehicle) {
		// Minimum travel time is calculated here. Note that we add one second, because
		// the QSim needs one second in any case to move a vehicle!
		double minimumTravelTime = Math.floor(link.getLength() / link.getFreespeed()) + 1.0;

		if (Time.isUndefinedTime(time)) {
			// Return minimum time if no time (= general case) is required
			return minimumTravelTime;
		}

		// Calculate travel time from TravelTimeCalculator (= recorded travel time)
		double calculatedTravelTime = delegate.getLinkTravelTime(link, time, person, vehicle);

		// Return maximum of minimum and recorded to get a proper prediction for the
		// QSim travel time
		return Math.max(calculatedTravelTime, minimumTravelTime);
	}
}
