package org.eqasim.core.scenario.cutter.outside;

import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class OutsideActivityAdapter {
	final private RoadNetwork roadNetwork;

	public OutsideActivityAdapter(RoadNetwork roadNetwork) {
		this.roadNetwork = roadNetwork;
	}

	public void run(Population population, ActivityFacilities facilities) {
		OutsideFacilityAdapter facilityAdapter = new OutsideFacilityAdapter(facilities);

		for (Person person : population.getPersons().values()) {
			for (Plan plan : person.getPlans()) {
				for (PlanElement element : plan.getPlanElements()) {
					if (element instanceof Activity) {
						Activity activity = (Activity) element;

						if (activity.getType().equals("outside")) {
							Link activityLink = null;

							if (activity.getLinkId() != null) {
								activityLink = roadNetwork.getLinks().get(activity.getLinkId());
							}
							// change this part because roadNetwork can in some cases not contain the activity link (a bike link for example)
							if (activityLink == null) {
								activityLink = NetworkUtils.getNearestLink(roadNetwork, activity.getCoord());
							}

							// check if activity link is still null, if so, we cannot do anything and just skip this activity
							if (activityLink == null) {
								throw new NullPointerException("The activity link is null for this activity. Here are the coordinates of the activity: "
										+ activity.getCoord() + ". \nThis should not happen, but we ignore the reason why this is happening for now, but it should be investigated in the future.");
							}

							ActivityFacility facility = facilityAdapter.getFacility(activityLink);

							activity.setCoord(facility.getCoord());
							activity.setLinkId(facility.getLinkId());
							activity.setFacilityId(facility.getId());
						}
					}
				}

				Activity lastActivity = (Activity) plan.getPlanElements().get(plan.getPlanElements().size() - 1);

				if (lastActivity.getType().equals("outside")) {
					lastActivity.setEndTimeUndefined();
				}
			}
		}
	}
}