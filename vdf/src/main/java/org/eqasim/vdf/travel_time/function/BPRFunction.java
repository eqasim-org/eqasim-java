package org.eqasim.vdf.travel_time.function;

import org.matsim.api.core.v01.network.Link;

public class BPRFunction implements VolumeDelayFunction {
	private final double factor;
	private final double exponent;

	public BPRFunction(double factor, double exponent) {
		this.factor = factor;
		this.exponent = exponent;
	}

	@Override
	public double getTravelTime(double time, double flow, double capacity, Link link) {
		double freeflowTravelTime = link.getLength() / link.getFreespeed(time);
		return freeflowTravelTime * (1.0 + factor * Math.pow(flow / capacity, exponent));
	}
}
