package org.eqasim.core.scenario.preparation;

import org.eqasim.core.misc.ParallelProgress;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;

public class LinkAssignment {
	private final ActivityFacilities facilities;

	public LinkAssignment(ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	public void run(Population population) throws InterruptedException {
		ParallelProgress progress = new ParallelProgress("Assigning facility links to activities ...",
				population.getPersons().size());
		progress.start();

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						Facility facility = facilities.getFacilities().get(activity.getFacilityId());

						if (facility == null) {
							throw new IllegalStateException(String.format("Facility %s for person %s does not exist!",
									activity.getFacilityId(), person.getId()));
						}

						if (facility.getLinkId() == null) {
							throw new IllegalStateException(
									String.format("Facility %s for person %s does not have a link!",
											activity.getFacilityId(), person.getId()));
						}

						activity.setLinkId(facility.getLinkId());
					}
				}
			}

			progress.update();
		}

		progress.close();
	}
}
