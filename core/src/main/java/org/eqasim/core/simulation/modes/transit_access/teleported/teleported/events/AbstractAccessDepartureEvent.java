package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.events;

import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

public class AbstractAccessDepartureEvent extends Event implements HasPersonId {

    public static final String EVENT_TYPE = "AbstractAccessArrivalEvent";
    private final Id<Person> personId;
    private final Id<AbstractAccessItem> accessItemId;
    private final Id<Link> departureLinkId;
    private final Id<Link> arrivalLinkId;
    private final boolean leavingAccessCenter;
    private final boolean isRouted;
    private final double distance;

    public AbstractAccessDepartureEvent(double time, Id<Person> personId, Id<AbstractAccessItem> accessItemId, Id<Link> departureLinkId, Id<Link> arrivalLinkId, boolean leavingAccessCenter, boolean isRouted, double distance) {
        super(time);
        this.personId = personId;
        this.accessItemId = accessItemId;
        this.departureLinkId = departureLinkId;
        this.arrivalLinkId = arrivalLinkId;
        this.leavingAccessCenter = leavingAccessCenter;
        this.isRouted = isRouted;
        this.distance = distance;
    }

    public Id<AbstractAccessItem> getAccessItemId() {
        return this.accessItemId;
    }
    public Id<Link> getDepartureLinkId() {
        return this.departureLinkId;
    }
    public Id<Link> getArrivalLinkId() {
        return this.arrivalLinkId;
    }

    public boolean isRouted() {
        return this.isRouted;
    }

    public double getDistance() {
        return this.distance;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @Override
    public Id<Person> getPersonId() {
        return this.personId;
    }

    public boolean isLeavingAccessCenter() {
        return leavingAccessCenter;
    }
}
