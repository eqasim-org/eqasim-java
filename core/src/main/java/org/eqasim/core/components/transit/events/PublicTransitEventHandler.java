package org.eqasim.core.components.transit.events;

import org.matsim.core.events.handler.EventHandler;

public interface PublicTransitEventHandler extends EventHandler {
    void handleEvent(PublicTransitEvent event);
}
