package org.eqasim.core.handlers;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;

public class TravelTimeItem {
    public Id<Person> personId;
    public Id<Link> linkId;
    public double enterTime;
    public double exitTime;

    public TravelTimeItem(Id<Person> personId, Id<Link> linkId, double enterTime, double exitTime) {
        this.personId = personId;
        this.linkId = linkId;
        this.enterTime = enterTime;
        this.exitTime = exitTime;
    }
}
