package org.eqasim.core.analysis.trips;

import java.util.Collection;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class TripReaderFromEvents {
	final private TripListener tripListener;

	public TripReaderFromEvents(TripListener tripListener) {
		this.tripListener = tripListener;
	}

	public Collection<TripItem> readTrips(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.addCustomEventMapper(PublicTransitEvent.TYPE, PublicTransitEvent::convert);
		matsimEventsReader.readFile(eventsPath);
		return tripListener.getTripItems();
	}
}
