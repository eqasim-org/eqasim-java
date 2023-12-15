package org.eqasim.core.scenario.cutter.outside;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class OutsideFacilityAdapter {
	final private ActivityFacilities facilities;
	final private Map<Link, ActivityFacility> facilitiesByLink = new HashMap<>();

	private int counter = 0;

	public OutsideFacilityAdapter(ActivityFacilities facilities) {
		this.facilities = facilities;
	}

	private Id<ActivityFacility> createNextId() {
		while (true) {
			counter++;
			var id = Id.create("outside_" + counter, ActivityFacility.class);
			if (!facilities.getFacilities().containsKey(id)) {
				return id;
			}
		}
	}

	public ActivityFacility getFacility(Link link) {
		ActivityFacility facility = facilitiesByLink.get(link);

		if (facility == null) {
			facility = facilities.getFactory().createActivityFacility(createNextId(), link.getCoord(), link.getId());

			ActivityOption option = facilities.getFactory().createActivityOption("outside");
			facility.addActivityOption(option);

			facilitiesByLink.put(link, facility);
			facilities.addActivityFacility(facility);
		}

		return facility;
	}
}
