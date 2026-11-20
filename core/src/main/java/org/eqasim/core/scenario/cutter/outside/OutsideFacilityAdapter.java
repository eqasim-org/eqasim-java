package org.eqasim.core.scenario.cutter.outside;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;

public class OutsideFacilityAdapter {
	final private ActivityFacilities facilities;
	private record Location(Link link, double x, double y) { }
	final private Map<Location, ActivityFacility> facilitiesByLink = new HashMap<>();

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
		return getFacility(link, link.getCoord());
	}

	/** Boundary activities sharing a link can still have different crossing coordinates. */
	public ActivityFacility getFacility(Link link, Coord coord) {
		if (link == null) {
			throw new NullPointerException("The link in null at this point. We ignore the reason why this is happening for now, but it should be investigated in the future.");
		}
		Location location = new Location(link, coord.getX(), coord.getY());
		ActivityFacility facility = facilitiesByLink.get(location);

		if (facility == null) {
			facility = facilities.getFactory().createActivityFacility(createNextId(), coord, link.getId());

			ActivityOption option = facilities.getFactory().createActivityOption("outside");
			facility.addActivityOption(option);

			facilitiesByLink.put(location, facility);
			facilities.addActivityFacility(facility);
		}

		return facility;
	}
}
