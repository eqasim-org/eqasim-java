package org.eqasim.flow;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class FlowTrip {
	public final Link originLink;
	public final Link destinationLink;

	double travelTime = Double.POSITIVE_INFINITY;
	Path path = null;

	public FlowTrip(Link originLink, Link destinationLink) {
		this.originLink = originLink;
		this.destinationLink = destinationLink;
	}

	public void updateTravelTime(double travelTime) {
		this.travelTime = travelTime;
	}

	public void updatePath(Path path) {
		this.path = path;
		this.travelTime = path.travelTime;
	}
}
