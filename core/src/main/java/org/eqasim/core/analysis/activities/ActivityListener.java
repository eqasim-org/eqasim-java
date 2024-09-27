package org.eqasim.core.analysis.activities;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

public class ActivityListener implements ActivityStartEventHandler, ActivityEndEventHandler {
	final private Collection<ActivityItem> activities = new LinkedList<>();
	final private Map<Id<Person>, ActivityItem> ongoing = new HashMap<>();
	final private Map<Id<Person>, Integer> activityIndex = new HashMap<>();

	final private PersonAnalysisFilter personFilter;

	public ActivityListener(PersonAnalysisFilter personFilter) {
		this.personFilter = personFilter;
	}

	public Collection<ActivityItem> getActivityItems() {
		return activities;
	}

	@Override
	public void reset(int iteration) {
		activities.clear();
		ongoing.clear();
		activityIndex.clear();
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				Integer personActivityIndex = activityIndex.get(event.getPersonId());

				if (personActivityIndex == null) {
					personActivityIndex = 0;
				} else {
					personActivityIndex = personActivityIndex + 1;
				}

				ongoing.put(event.getPersonId(),
						new ActivityItem(event.getPersonId(), personActivityIndex, event.getActType(), event.getTime(),
								Double.NaN, event.getCoord().getX(), event.getCoord().getY()));

				activityIndex.put(event.getPersonId(), personActivityIndex);
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				ActivityItem activity = ongoing.remove(event.getPersonId());

				if (activity != null) {
					activity.endTime = event.getTime();
					activities.add(activity);
				}
			}
		}
	}
}