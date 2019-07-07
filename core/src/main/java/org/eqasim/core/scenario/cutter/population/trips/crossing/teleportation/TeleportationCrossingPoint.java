package org.eqasim.core.scenario.cutter.population.trips.crossing.teleportation;

import org.matsim.api.core.v01.Coord;

public class TeleportationCrossingPoint {
	final public Coord coord;
	final public double time;
	final public boolean isOutgoing;

	public TeleportationCrossingPoint(Coord coord, double time, boolean isOutgoing) {
		this.coord = coord;
		this.time = time;
		this.isOutgoing = isOutgoing;
	}
}
