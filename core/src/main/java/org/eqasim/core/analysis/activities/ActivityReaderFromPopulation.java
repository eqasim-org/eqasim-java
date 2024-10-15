package org.eqasim.core.analysis.activities;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.config.groups.PlansConfigGroup.TripDurationHandling;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.core.utils.timing.TimeTracker;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class ActivityReaderFromPopulation {
	final private PersonAnalysisFilter personFilter;
	final private Optional<Network> network;
	final private Optional<ActivityFacilities> facilities;

	public ActivityReaderFromPopulation(PersonAnalysisFilter personFilter, Optional<Network> network,
			Optional<ActivityFacilities> facilities) {
		this.personFilter = personFilter;
		this.network = network;
		this.facilities = facilities;
	}

	public Collection<ActivityItem> readActivities(String populationPath) {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);
		return readActivities(scenario.getPopulation());
	}

	private Coord getCoordinate(Id<Person> personId, Activity activity) {
		Id<ActivityFacility> facilityId = activity.getFacilityId();
		Id<Link> linkId = activity.getLinkId();
		Coord coord = activity.getCoord();

		if (facilityId != null && facilities.isPresent()) {
			ActivityFacility facility = facilities.get().getFacilities().get(facilityId);

			if (facility == null) {
				throw new IllegalStateException("Could not find facility: " + facilityId);
			} else {
				coord = facility.getCoord();
				linkId = facility.getLinkId();
			}
		}

		if (linkId != null && network.isPresent()) {
			Link link = network.get().getLinks().get(linkId);

			if (link == null) {
				throw new IllegalStateException("Could not find link: " + linkId);
			} else {
				coord = link.getCoord();
			}
		}

		if (coord == null) {
			throw new IllegalStateException("Could not find coordinate for activity of " + personId);
		}

		return coord;
	}

	public Collection<ActivityItem> readActivities(Population population) {
		List<ActivityItem> activityItems = new LinkedList<>();

		TimeInterpretation timeInterpretation = TimeInterpretation
				.create(ActivityDurationInterpretation.tryEndTimeThenDuration, TripDurationHandling.ignoreDelays);

		for (Person person : population.getPersons().values()) {
			if (personFilter.analyzePerson(person.getId())) {
				TimeTracker tracker = new TimeTracker(timeInterpretation);
				int personActivityIndex = 0;

				for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
					double startTime = tracker.getTime().seconds();
					tracker.addElement(element);
					double endTime = tracker.getTime().seconds();

					if (element instanceof Activity activity) {
						if (!TripStructureUtils.isStageActivityType(activity.getType())) {
							Coord location = getCoordinate(person.getId(), activity);

							if (activity.getStartTime().isDefined()) {
								tracker.setTime(activity.getStartTime().seconds());
							}

							activityItems.add(new ActivityItem(person.getId(), personActivityIndex, activity.getType(),
									startTime, endTime, location.getX(), location.getY()));

							personActivityIndex++;
						}
					}
				}
			}
		}

		return activityItems;
	}
}
