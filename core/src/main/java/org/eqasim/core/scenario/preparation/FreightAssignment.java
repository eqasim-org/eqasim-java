package org.eqasim.core.scenario.preparation;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * TODO: Eventually, this should happen in the pipeline. We assign facility IDs
 * to the freight activities here.
 */
public class FreightAssignment {
	private final QuadTree<ActivityFacility> index;

	public FreightAssignment(Network network, ActivityFacilities facilities) {
		double[] bounds = NetworkUtils.getBoundingBox(network.getNodes().values());
		index = new QuadTree<>(bounds[0], bounds[1], bounds[2], bounds[3]);

		for (ActivityFacility facility : facilities.getFacilities().values()) {
			Link link = network.getLinks().get(facility.getLinkId());
			index.put(link.getCoord().getX(), link.getCoord().getY(), facility);
		}
	}

	public void run(Population population) {
		for (Person person : population.getPersons().values()) {
			Boolean isFreight = (Boolean) person.getAttributes().getAttribute("isFreight");

			if (isFreight != null && isFreight) {
				for (Plan plan : person.getPlans()) {
					for (PlanElement element : plan.getPlanElements()) {
						if (element instanceof Activity) {
							Activity activity = (Activity) element;
							ActivityFacility facility = index.getClosest(activity.getCoord().getX(),
									activity.getCoord().getY());

							activity.setFacilityId(facility.getId());
							activity.setCoord(facility.getCoord());
						}
					}
				}
			}
		}
	}
}
