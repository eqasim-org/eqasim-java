package org.eqasim.core.travel_times.items;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

public class TripTravelTimeItem {
    public Id<Person> personId;
    public int tripId;
    public double travelTime;

    public TripTravelTimeItem(Id<Person> personId, int tripId, double travelTime) {
        this.personId = personId;
        this.tripId = tripId;
        this.travelTime = travelTime;
    }

    public static String formatHeader() {
        return String.join(";", new String[] {
                "person_id", "trip_id", "travel_time"
        });
    }

    public String formatItem() {
        return String.join(";", new String[] {
                this.personId.toString(),
                Integer.toString(this.tripId),
                Double.toString(this.travelTime),
        });
    }
}
