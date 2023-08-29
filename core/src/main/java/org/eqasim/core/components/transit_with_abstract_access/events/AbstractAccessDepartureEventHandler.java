package org.eqasim.core.components.transit_with_abstract_access.events;

import org.matsim.core.events.handler.EventHandler;

public interface AbstractAccessDepartureEventHandler extends EventHandler {
    void handleEvent(AbstractAccessDepartureEvent event);
}
