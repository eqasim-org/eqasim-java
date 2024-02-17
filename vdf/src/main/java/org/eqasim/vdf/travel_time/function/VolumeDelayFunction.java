package org.eqasim.vdf.travel_time.function;

import org.matsim.api.core.v01.network.Link;

public interface VolumeDelayFunction {
	double getTravelTime(double time, double flow, double capacity, Link link);
}
