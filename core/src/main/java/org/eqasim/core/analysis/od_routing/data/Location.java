package org.eqasim.core.analysis.od_routing.data;

import org.matsim.api.core.v01.BasicLocation;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

public class Location implements Identifiable<Location>, BasicLocation {
	private final Id<Location> id;
	private final Coord coord;

	public Location(Id<Location> id, Coord coord) {
		this.id = id;
		this.coord = coord;
	}

	@Override
	public Id<Location> getId() {
		return id;
	}

	@Override
	public Coord getCoord() {
		return coord;
	}
}
