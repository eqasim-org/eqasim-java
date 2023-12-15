package org.eqasim.examples.corsica_drt.rejections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestSubmittedEventHandler;

public class RejectionTracker implements PassengerRequestSubmittedEventHandler, PassengerRequestRejectedEventHandler {
	private IdMap<Person, Integer> numberOfRequests = new IdMap<>(Person.class);
	private IdMap<Person, Integer> numberOfRejections = new IdMap<>(Person.class);

	private int defaultRequests = 10;
	private int defaultRejections = 0;

	@Override
	public void handleEvent(PassengerRequestRejectedEvent event) {
		for (Id<Person> personId : event.getPersonIds()) {
			numberOfRejections.compute(personId, (k, v) -> v == null ? defaultRejections + 1 : v + 1);
		}
	}

	@Override
	public void handleEvent(PassengerRequestSubmittedEvent event) {
		for (Id<Person> personId : event.getPersonIds()) {
			numberOfRequests.compute(personId, (k, v) -> v == null ? defaultRequests + 1 : v + 1);
		}
	}

	public double getRejectionProbability(Id<Person> personId) {
		return numberOfRejections.getOrDefault(personId, defaultRejections)
				/ numberOfRequests.getOrDefault(personId, defaultRequests);
	}

	@Override
	public void reset(int iteration) {
	}
}
