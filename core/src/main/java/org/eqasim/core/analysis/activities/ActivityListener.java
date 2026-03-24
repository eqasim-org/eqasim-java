package org.eqasim.core.analysis.activities;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonInitializedEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonInitializedEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.TripStructureUtils;

import com.google.common.base.Verify;

public class ActivityListener
		implements PersonInitializedEventHandler, ActivityStartEventHandler, ActivityEndEventHandler {
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
	public void handleEvent(PersonInitializedEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			ActivityItem activity = new ActivityItem(event.getPersonId(), 0, event.getActivityType(),
					Double.NEGATIVE_INFINITY,
					Double.POSITIVE_INFINITY, event.getCoord().getX(), event.getCoord().getY(), event.getFacilityId(),
					event.getLinkId());

			activities.add(activity);
			ongoing.put(event.getPersonId(), activity);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				int personActivityIndex = activityIndex.compute(event.getPersonId(),
						(id, val) -> val == null ? 1 : val + 1);

				ActivityItem activity = new ActivityItem(event.getPersonId(), personActivityIndex, event.getActType(),
						event.getTime(), Double.POSITIVE_INFINITY, event.getCoord().getX(), event.getCoord().getY(),
						event.getFacilityId(), event.getLinkId());

				activities.add(activity);
				Verify.verify(ongoing.put(event.getPersonId(), activity) == null);
			}
		}
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				ActivityItem activity = Objects.requireNonNull(ongoing.remove(event.getPersonId()),
						"Are you running activity analysis on an instance without qsim.personInitializedEvents = 'all'?");
				activity.endTime = event.getTime();
			}
		}
	}
}