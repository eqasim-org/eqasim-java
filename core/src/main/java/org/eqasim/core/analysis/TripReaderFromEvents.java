package org.eqasim.core.analysis;

import java.util.Collection;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class TripReaderFromEvents {
	final private TripListener tripListener;
	private Collection<TripItem> tripItems;
	private Collection<LegItem> legItems;

	public TripReaderFromEvents(TripListener tripListener) {
		this.tripListener = tripListener;
	}

	public Collection<TripItem> getTripItems() {
		return tripItems;
	}

	public Collection<LegItem> getLegItems() {
		return legItems;
	}

	public void read(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);
		new MatsimEventsReader(eventsManager).readFile(eventsPath);
		tripItems = tripListener.getTripItems();
		legItems = tripListener.getLegItems();
	}
}
