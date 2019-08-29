package org.eqasim.core.travel_times.handlers;

import org.eqasim.core.items.LinkTraverseTimeItem;
import org.eqasim.core.travel_times.PredictedTravelTimeWriter;
import org.eqasim.core.travel_times.items.TripTravelTimeItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.algorithms.Vehicle2DriverEventHandler;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TravelTimeHandler implements LinkEnterEventHandler, LinkLeaveEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler,
        PersonDepartureEventHandler, PersonArrivalEventHandler,
        ActivityEndEventHandler,
        IterationStartsListener, IterationEndsListener {

    private Scenario scenario;

    // link traverse time
    private Collection<LinkTraverseTimeItem> linkTraverseTimeItems = new LinkedList<>();
    private Map<Id<Person>, Double> personEnterTimes = new HashMap<>();
    private Collection<Id<Link>> linkIds;
    private Vehicle2DriverEventHandler vehicle2DriverEventHandler;

    // trip experienced travel time
    private Collection<TripTravelTimeItem> tripTravelTimeItems = new LinkedList<>();
    private Map<Id<Person>, Integer> personTripCount = new HashMap<>();
    private Map<Id<Person>, Double> personTripStartTimes = new HashMap<>();

    public TravelTimeHandler(Scenario scenario, Collection<Id<Link>> linkIds, Vehicle2DriverEventHandler vehicle2DriverEventHandler) {
        this.scenario = scenario;
        this.linkIds = linkIds;
        this.vehicle2DriverEventHandler = vehicle2DriverEventHandler;

        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personEnterTimes.putIfAbsent(personId, 0.0);
            this.personTripCount.putIfAbsent(personId, -1);
            this.personTripStartTimes.putIfAbsent(personId, 0.0);
        }
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        String outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "predicted_travel_times.csv");
        PredictedTravelTimeWriter.initialize(outputPath);
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.personTripStartTimes.put(event.getPersonId(), event.getTime());
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = event.getPersonId();
            this.personEnterTimes.put(personId, event.getTime());
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
    public void handleEvent(LinkLeaveEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = vehicle2DriverEventHandler.getDriverOfVehicle(event.getVehicleId());
            Id<Link> linkId = event.getLinkId();
            double enterTime = this.personEnterTimes.get(personId);

            // add travel time item
            LinkTraverseTimeItem linkTraverseTimeItem = new LinkTraverseTimeItem(personId, linkId, enterTime, event.getTime());
            this.linkTraverseTimeItems.add(linkTraverseTimeItem);

            // set enter time to link exit time
            this.personEnterTimes.put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if (linkIds.contains(event.getLinkId())) {
            Id<Person> personId = event.getPersonId();
            Id<Link> linkId = event.getLinkId();
            double enterTime = this.personEnterTimes.get(personId);

            // add travel time item
            LinkTraverseTimeItem linkTraverseTimeItem = new LinkTraverseTimeItem(personId, linkId, enterTime, event.getTime());
            this.linkTraverseTimeItems.add(linkTraverseTimeItem);

            // set enter time to link exit time
            this.personEnterTimes.put(personId, event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        if (event.getLegMode().equals(TransportMode.car)) {
            Id<Person> personId = event.getPersonId();
            double startTime = this.personTripStartTimes.get(personId);
            double travelTime = event.getTime() - startTime;

            // add travel time item
            int tripId = personTripCount.get(personId);
            TripTravelTimeItem tripTravelTimeItem = new TripTravelTimeItem(personId, tripId, travelTime);
            this.tripTravelTimeItems.add(tripTravelTimeItem);
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (!event.getEventType().contains("interaction")) {
            int count = this.personTripCount.get(event.getPersonId());
            this.personTripCount.put(event.getPersonId(), count + 1);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        writeLinkTraverseTimes(event
                .getServices()
                .getControlerIO()
                .getIterationFilename(event.getIteration(), "link_traverse_times.csv")
        );
        writeTripExperiencedTravelTimes(event
                .getServices()
                .getControlerIO()
                .getIterationFilename(event.getIteration(), "experienced_travel_times.csv")
        );

        PredictedTravelTimeWriter.close();
    }

    public Collection<LinkTraverseTimeItem> getLinkTraverseTimeItems() {
        return linkTraverseTimeItems;
    }

    public Collection<Id<Link>> getLinkIds() {
        return linkIds;
    }

    public Collection<TripTravelTimeItem> getTripTravelTimeItems() {
        return tripTravelTimeItems;
    }


    @Override
    public void reset(int iteration) {
        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personEnterTimes.put(personId, 0.0);
            this.personTripCount.put(personId, -1);
            this.personTripStartTimes.put(personId, 0.0);
        }
        this.linkTraverseTimeItems.clear();
        this.tripTravelTimeItems.clear();
    }

    private void writeLinkTraverseTimes(String outputPath) {

        try {
            System.out.println("Writing link traverse times to " + outputPath);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

            writer.write(LinkTraverseTimeItem.formatHeader() + "\n");
            writer.flush();

            Counter counter = new Counter("Traverse #");

            for (LinkTraverseTimeItem linkTraverseTimeItem : this.linkTraverseTimeItems) {

                writer.write(linkTraverseTimeItem.formatItem() + "\n");
                writer.flush();

                counter.incCounter();
            }

            writer.flush();
            writer.close();
            counter.printCounter();

            System.out.println("Done");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void writeTripExperiencedTravelTimes(String outputPath) {

        try {
            System.out.println("Writing trip travel times to " + outputPath);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

            writer.write(TripTravelTimeItem.formatHeader() + "\n");
            writer.flush();

            Counter counter = new Counter("Trip #");

            for (TripTravelTimeItem tripTravelTimeItem : this.tripTravelTimeItems) {

                writer.write(tripTravelTimeItem.formatItem() + "\n");
                writer.flush();

                counter.incCounter();
            }

            writer.flush();
            writer.close();
            counter.printCounter();

            System.out.println("Done");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
