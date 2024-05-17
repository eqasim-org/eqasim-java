package org.eqasim.core.analysis.cba.analyzers.rideAnalysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;

import java.util.ArrayList;
import java.util.List;

public class RideTrip {
    Id<Person> personId;
    ActivityEndEvent previousActivityEnd;
    ActivityStartEvent nextActivityStart;
    List<ActivityEndEvent> interactionActivitiesEndEvents = new ArrayList<>();
    List<PersonDepartureEvent> personDepartureEvents = new ArrayList<>();
    List<TeleportationArrivalEvent> teleportationArrivalEvents = new ArrayList<>();
    List<PersonArrivalEvent> personArrivalEvents = new ArrayList<>();

    public boolean validate() {
        boolean hasRide = false;
        for(PersonDepartureEvent personDepartureEvent : this.personDepartureEvents) {
            if(personDepartureEvent.getLegMode().equals("ride")) {
                hasRide = true;
                break;
            }
        }
        if(!hasRide) {
            return false;
        }
        for(ActivityEndEvent event : this.interactionActivitiesEndEvents) {
            if(!event.getActType().equals("ride interaction")) {
                throw new IllegalStateException("Trip contains a ride departure as well as an interaction activity with another mode (" + event.getActType() + ")");
            }
        }
        if(this.interactionActivitiesEndEvents.size() > 2) {
            throw new IllegalStateException("a single trip contains more than two ride interaction activities");
        }
        if (personDepartureEvents.size() != 3 || !personDepartureEvents.get(0).getLegMode().equals("walk") || !personDepartureEvents.get(1).getLegMode().equals("ride") || !personDepartureEvents.get(0).getLegMode().equals("walk")) {
            throw new IllegalStateException("Ride trips should contain three departures with modes : walk, ride, walk");
        }
        if(personArrivalEvents.size() != 3 || !personArrivalEvents.get(0).getLegMode().equals("walk") || !personArrivalEvents.get(1).getLegMode().equals("ride") || !personArrivalEvents.get(2).getLegMode().equals("walk")) {
            throw new IllegalStateException("The three departures with modes walk, ride, walk should all have respective teleportation arrival events");
        }
        return true;
    }

    public String getPurpose(){
        return this.previousActivityEnd.getActType() + " -- " + nextActivityStart.getActType();
    }

    public double getAccessTime() {
        return this.teleportationArrivalEvents.get(0).getTime() - this.personDepartureEvents.get(0).getTime();
    }

    public double getAccessDistance() {
        return this.teleportationArrivalEvents.get(0).getDistance();
    }

    public double getEgressTime() {
        return this.teleportationArrivalEvents.get(2).getTime() - this.personDepartureEvents.get(2).getTime();
    }

    public double getEgressDistance() {
        return this.teleportationArrivalEvents.get(2).getDistance();
    }

    public double getRideTime() {
        return this.teleportationArrivalEvents.get(1).getTime() - this.personDepartureEvents.get(1).getTime();
    }

    public double getRideDistance() {
        return this.teleportationArrivalEvents.get(1).getDistance();
    }

    public double getDepartureTime() {
        return this.personDepartureEvents.get(0).getTime();
    }

    public double getArrivalTime() {
        return this.nextActivityStart.getTime();
    }
}
