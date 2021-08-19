package org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing;

public class LinkTimingData {
	public final double enterTime;
	public final double leaveTime;
	public final int legIndex;

	LinkTimingData(double enterTime, double leaveTime, int legIndex) {
		this.enterTime = enterTime;
		this.leaveTime = leaveTime;
		this.legIndex = legIndex;
	}
}
