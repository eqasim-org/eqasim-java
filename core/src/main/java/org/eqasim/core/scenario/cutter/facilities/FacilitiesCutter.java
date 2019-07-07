package org.eqasim.core.scenario.cutter.facilities;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class FacilitiesCutter {
	private final static Logger log = Logger.getLogger(FacilitiesCutter.class);

	private final ScenarioExtent extent;
	private final Collection<Id<ActivityFacility>> usedFacilityIds = new HashSet<>();

	public FacilitiesCutter(ScenarioExtent extent, Population population) {
		this.extent = extent;

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;
						usedFacilityIds.add(activity.getFacilityId());
					}
				}
			}
		}
	}

	public void run(ActivityFacilities facilities, boolean keepAllInside) {
		log.info("Cutting facilities ...");
		int originalNumberOfFacilities = facilities.getFacilities().size();

		Iterator<? extends ActivityFacility> iterator = facilities.getFacilities().values().iterator();

		while (iterator.hasNext()) {
			ActivityFacility facility = iterator.next();

			boolean keep = usedFacilityIds.contains(facility.getId())
					|| (extent.isInside(facility.getCoord()) && keepAllInside);

			if (!keep) {
				iterator.remove();
			}
		}

		int finalNumberOfFacilities = facilities.getFacilities().size();

		log.info("Number of facilities before: " + originalNumberOfFacilities);
		log.info("Number of facilities now: " + finalNumberOfFacilities);
	}
}
