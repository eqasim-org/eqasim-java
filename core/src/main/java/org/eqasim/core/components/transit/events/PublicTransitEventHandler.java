package org.eqasim.core.components.transit.events;

import org.matsim.core.events.handler.EventHandler;

public interface PublicTransitEventHandler extends EventHandler {
	public void handleEvent(PublicTransitEvent event);
}
