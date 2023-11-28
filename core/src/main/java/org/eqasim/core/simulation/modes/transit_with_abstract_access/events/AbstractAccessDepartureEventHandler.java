package org.eqasim.core.simulation.modes.transit_with_abstract_access.events;

import org.matsim.core.events.handler.EventHandler;

public interface AbstractAccessDepartureEventHandler extends EventHandler {
    void handleEvent(AbstractAccessDepartureEvent event);
}
