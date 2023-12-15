package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.events;

import org.matsim.core.events.handler.EventHandler;

public interface AbstractAccessDepartureEventHandler extends EventHandler {
    void handleEvent(AbstractAccessDepartureEvent event);
}
