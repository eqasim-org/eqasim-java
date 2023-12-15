package org.eqasim.core.analysis.legs;

import java.util.Collection;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class LegReaderFromEvents {
	final private LegListener legListener;

	public LegReaderFromEvents(LegListener legListener) {
		this.legListener = legListener;
	}

	public Collection<LegItem> readLegs(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(legListener);
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.addCustomEventMapper(PublicTransitEvent.TYPE, PublicTransitEvent::convert);
		matsimEventsReader.readFile(eventsPath);
		return legListener.getLegItems();
	}
}
