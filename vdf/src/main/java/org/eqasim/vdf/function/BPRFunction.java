package org.eqasim.vdf.function;

import org.matsim.api.core.v01.network.Link;

public class BPRFunction implements VolumeDelayFunction {
	@Override
	public double getTravelTime(double time, double flow, double capacity, Link link) {
		double freeflowTravelTime = link.getLength() / link.getFreespeed(time);
		return freeflowTravelTime * (1.0 + 0.15 * Math.pow(flow / capacity, 4));
	}
}
