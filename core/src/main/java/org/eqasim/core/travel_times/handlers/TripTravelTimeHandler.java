package org.eqasim.core.travel_times.handlers;

import org.eqasim.core.travel_times.items.TripTravelTimeItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.misc.Counter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TripTravelTimeHandler implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler, ActivityEndEventHandler, IterationEndsListener {

    private Scenario scenario;
    private Collection<TripTravelTimeItem> tripTravelTimeItems = new LinkedList<>();
    private Map<Id<Person>, Integer> personTripCount = new HashMap<>();
    private Map<Id<Person>, Double> personTripStartTimes = new HashMap<>();

    public TripTravelTimeHandler(Scenario scenario) {
        this.scenario = scenario;
        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personTripCount.putIfAbsent(personId, -1);
            this.personTripStartTimes.putIfAbsent(personId, 0.0);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.personTripStartTimes.put(event.getPersonId(), event.getTime());
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
    public void handleEvent(ActivityStartEvent event) {

    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        String outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "experienced_travel_times.csv");
        write(outputPath);
    }

    public Collection<TripTravelTimeItem> getTripTravelTimeItems() {
        return tripTravelTimeItems;
    }

    @Override
    public void reset(int iteration) {
        for (Id<Person> personId : scenario.getPopulation().getPersons().keySet()) {
            this.personTripCount.put(personId, -1);
            this.personTripStartTimes.put(personId, 0.0);
        }
        this.tripTravelTimeItems.clear();
    }

    private void write(String outputPath) {

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
