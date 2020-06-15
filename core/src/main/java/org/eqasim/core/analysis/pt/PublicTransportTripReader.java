package org.eqasim.core.analysis.pt;

import java.util.Collection;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class PublicTransportTripReader {
	final private PublicTransportTripListener tripListener;

	public PublicTransportTripReader(PublicTransportTripListener tripListener) {
		this.tripListener = tripListener;
	}

	public Collection<PublicTransportTripItem> readTrips(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
		reader.readFile(eventsPath);

		return tripListener.getTripItems();
	}
}
