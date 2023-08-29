package org.eqasim.core.components.transit_with_abstract_access.events;

import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.network.Link;

public class AbstractAccessDepartureEvent extends Event {

    public static final String EVENT_TYPE = "AbstractAccessArrivalEvent";

    private final Id<AbstractAccessItem> accessItemId;

    private final Id<Link> departureLinkId;

    private final Id<Link> arrivalLinkId;

    public AbstractAccessDepartureEvent(double time, Id<AbstractAccessItem> accessItemId, Id<Link> departureLinkId, Id<Link> arrivalLinkId) {
        super(time);
        this.accessItemId = accessItemId;
        this.departureLinkId = departureLinkId;
        this.arrivalLinkId = arrivalLinkId;
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

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }
}
