package org.eqasim.core.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TravelTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler, PersonDepartureEventHandler, PersonArrivalEventHandler, IterationEndsListener {

    private Scenario scenario;
    private Collection<TravelTimeItem> travelTimeItems = new LinkedList<>();
    private Map<Id<Person>, Double> personEnterTimes = new HashMap<>();
    private Collection<Id<Link>> linkIds;
    private Vehicle2DriverEventHandler vehicle2DriverEventHandler;

    public TravelTimeHandler(Collection<Id<Link>> linkIds, Scenario scenario, Vehicle2DriverEventHandler vehicle2DriverEventHandler) {
        this.linkIds = linkIds;
        this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;

        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personEnterTimes.putIfAbsent(personId, 0.0);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
            this.personEnterTimes.put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            this.personEnterTimes.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
            Id<Link> linkId = event.getLinkId();
            double enterTime = this.personEnterTimes.get(personId);

            // add travel time item
            TravelTimeItem travelTimeItem = new TravelTimeItem(personId, linkId, enterTime, event.getTime());
            this.travelTimeItems.add(travelTimeItem);

            // set enter time to link exit time
            this.personEnterTimes.put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = event.getPersonId();
            Id<Link> linkId = event.getLinkId();
            double enterTime = this.personEnterTimes.get(personId);

            // add travel time item
            TravelTimeItem travelTimeItem = new TravelTimeItem(personId, linkId, enterTime, event.getTime());
            this.travelTimeItems.add(travelTimeItem);

            // set enter time to link exit time
            this.personEnterTimes.put(personId, event.getTime());
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "travel_times.csv");
    }

    public Collection<TravelTimeItem> getTravelTimeItems() {
        return travelTimeItems;
    }

    public Collection<Id<Link>> getLinkIds() {
        return linkIds;
    }

    @Override
    public void reset(int iteration) {
        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personEnterTimes.putIfAbsent(personId, 0.0);
        }
        this.travelTimeItems.clear();
    }
}
