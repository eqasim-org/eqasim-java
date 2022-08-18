package org.eqasim.core.analysis.pt;

import java.util.Collection;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class PublicTransportLegReader {
	final private PublicTransportLegListener tripListener;

	public PublicTransportLegReader(PublicTransportLegListener tripListener) {
		this.tripListener = tripListener;
	}

	public Collection<PublicTransportLegItem> readTrips(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);

		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
		reader.readFile(eventsPath);

		return tripListener.getTripItems();
	}
}
