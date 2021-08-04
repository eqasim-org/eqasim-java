package org.eqasim.core.scenario.cutter.population.trips.crossing.network.event_based;

public class CrossingTime {
	public final double enterTime;
	public final double leaveTime;

	CrossingTime(double enterTime, double leaveTime) {
		this.enterTime = enterTime;
		this.leaveTime = leaveTime;
	}
}
