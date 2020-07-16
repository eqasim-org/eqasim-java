package org.eqasim.core.analysis.od_routing.data;

import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.facilities.Facility;

public class LocationFacility implements Facility {
	private final Location location;

	public LocationFacility(Location location) {
		this.location = location;
	}

	@Override
	public Coord getCoord() {
		return location.getCoord();
	}

	@Override
	public Map<String, Object> getCustomAttributes() {
		return Collections.emptyMap();
	}

	@Override
	public Id<Link> getLinkId() {
		return null;
	}
}
