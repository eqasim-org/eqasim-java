package org.eqasim.core.simulation.modes.drt.mode_choice.rejections;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEvent;
import org.matsim.contrib.dvrp.passenger.PassengerRequestRejectedEventHandler;

public class RejectionTracker implements PassengerRequestRejectedEventHandler {
    private final IdMap<Person, Integer> rejections = new IdMap<>(Person.class);

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        synchronized (rejections) {
            for (Id<Person> personId : event.getPersonIds()) {
                rejections.put(personId, rejections.getOrDefault(personId, 0) + 1);
            }
        }
    }

    public int getRejections(Id<Person> personId) {
        return rejections.getOrDefault(personId, 0);
    }
}
