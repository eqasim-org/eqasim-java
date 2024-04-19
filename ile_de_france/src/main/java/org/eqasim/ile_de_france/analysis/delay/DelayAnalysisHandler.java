package org.eqasim.ile_de_france.analysis.delay;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

import com.google.common.base.Verify;

class DelayAnalysisHandler implements ActivityStartEventHandler, ActivityEndEventHandler {
	private final PersonAnalysisFilter personFilter;
	private final Population population;

	private final IdMap<Person, DelayItem> ongoing = new IdMap<>(Person.class);
	private final IdMap<Person, Integer> indices = new IdMap<>(Person.class);

	private final BufferedWriter writer;

	public DelayAnalysisHandler(Population population, PersonAnalysisFilter personFilter, BufferedWriter writer) {
		this.writer = writer;
		this.personFilter = personFilter;
		this.population = population;

		try {
			writer.write(String.join(";", Arrays.asList( //
					"personId", //
					"activityIndex", //
					"plannedStartTime", //
					"plannedEndTime", //
					"simulatedStartTime", //
					"simulatedEndTime" //
			)) + "\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (personFilter.analyzePerson(event.getPersonId()) && !event.getPersonId().toString().startsWith("drt")) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				int activityIndex = indices.compute(event.getPersonId(), (id, index) -> index == null ? 0 : index + 1);
				Verify.verify(activityIndex > 0);

				Plan plan = population.getPersons().get(event.getPersonId()).getSelectedPlan();
				List<Activity> activities = TripStructureUtils.getActivities(plan,
						StageActivityHandling.ExcludeStageActivities);
				Activity activity = activities.get(activityIndex);

				DelayItem item = createItem(event.getPersonId(), activityIndex, activity);
				item.simulatedStartTime = event.getTime();

				ongoing.put(event.getPersonId(), item);
			}
		}
	}

	private DelayItem createItem(Id<Person> personId, int activityIndex, Activity activity) {
		DelayItem item = new DelayItem();

		item.personId = personId;
		item.activityIndex = activityIndex;
		item.plannedStartTime = activity.getStartTime().orElse(Double.NEGATIVE_INFINITY);
		item.plannedEndTime = activity.getEndTime().orElse(Double.POSITIVE_INFINITY);
		item.simulatedStartTime = Double.POSITIVE_INFINITY;
		item.simulatedEndTime = Double.POSITIVE_INFINITY;

		return item;
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personFilter.analyzePerson(event.getPersonId()) && !event.getPersonId().toString().startsWith("drt")) {
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				DelayItem item = ongoing.remove(event.getPersonId());
				int activityIndex = indices.computeIfAbsent(event.getPersonId(), id -> 0);

				Verify.verify(item == null ^ activityIndex > 0);

				if (item == null) {
					Plan plan = population.getPersons().get(event.getPersonId()).getSelectedPlan();
					Activity activity = TripStructureUtils
							.getActivities(plan, StageActivityHandling.ExcludeStageActivities).get(0);

					item = createItem(event.getPersonId(), 0, activity);
					item.simulatedStartTime = Double.NEGATIVE_INFINITY;
				}

				item.simulatedEndTime = event.getTime();
				writeItem(item);
			}
		}
	}

	public void finish() {
		for (DelayItem item : ongoing.values()) {
			writeItem(item);
		}

		for (Person person : population.getPersons().values()) {
			Integer startIndex = indices.remove(person.getId());

			if (startIndex == null) {
				startIndex = 0;
			} else {
				startIndex += 1;
			}

			List<Activity> activities = TripStructureUtils.getActivities(person.getSelectedPlan(),
					StageActivityHandling.ExcludeStageActivities);

			for (int i = startIndex; i < activities.size(); i++) {
				writeItem(createItem(person.getId(), i, activities.get(i)));
			}
		}
	}

	private void writeItem(DelayItem item) {
		try {
			writer.write(String.join(";", Arrays.asList( //
					item.personId.toString(), //
					String.valueOf(item.activityIndex), //
					String.valueOf(item.plannedStartTime), //
					String.valueOf(item.plannedEndTime), //
					String.valueOf(item.simulatedStartTime), //
					String.valueOf(item.simulatedEndTime) //
			)) + "\n");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
