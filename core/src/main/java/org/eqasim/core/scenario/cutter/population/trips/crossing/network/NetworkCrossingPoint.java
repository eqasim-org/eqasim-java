package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import org.matsim.api.core.v01.network.Link;

public class NetworkCrossingPoint {
	final public Link link;
	final public int index;

	final public double enterTime;
	final public double leaveTime;
	
	final public boolean isOutgoing;

	public NetworkCrossingPoint(int index, Link link, double enterTime, double leaveTime, boolean isOutgoing) {
		this.index = index;
		this.link = link;
		this.enterTime = enterTime;
		this.leaveTime = leaveTime;
		this.isOutgoing = isOutgoing;
	}
}
