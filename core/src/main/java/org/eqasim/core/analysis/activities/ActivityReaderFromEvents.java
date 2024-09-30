package org.eqasim.core.analysis.activities;

import java.util.Collection;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;

public class ActivityReaderFromEvents {
	final private ActivityListener activityListener;

	public ActivityReaderFromEvents(ActivityListener activityListener) {
		this.activityListener = activityListener;
	}

	public Collection<ActivityItem> readActivities(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(activityListener);
		MatsimEventsReader matsimEventsReader = new MatsimEventsReader(eventsManager);
		matsimEventsReader.readFile(eventsPath);
		return activityListener.getActivityItems();
	}
}
