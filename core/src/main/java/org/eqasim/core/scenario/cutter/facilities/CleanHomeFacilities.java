package org.eqasim.core.scenario.cutter.facilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class CleanHomeFacilities {
	private final Collection<Id<ActivityFacility>> retainedIds = new HashSet<>();

	public CleanHomeFacilities(Population population) {
		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (activity.getType().equals("home")) {
							retainedIds.add(activity.getFacilityId());
						}
					}
				}
			}
		}
	}

	public void run(ActivityFacilities facilities) {
		Iterator<? extends ActivityFacility> iterator = facilities.getFacilities().values().iterator();

		while (iterator.hasNext()) {
			ActivityFacility facility = iterator.next();

			boolean hasHome = false;
			boolean hasOthers = false;
			
			for (ActivityOption option : facility.getActivityOptions().values()) {
				if (!option.getType().equals("home")) {
					hasOthers = true;
				} else {
					hasHome = true;
				}
			}
			
			boolean isCandidateForRemoval = hasHome && !hasOthers;

			if (isCandidateForRemoval && !retainedIds.contains(facility.getId())) {
				iterator.remove();
			}
		}
	}
}
