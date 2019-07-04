package org.eqasim.location_assignment.algorithms.discretizer;

import org.eqasim.location_assignment.assignment.discretization.DiscreteLocation;

public class DefaultDiscretizationResult implements DiscretizerResult {
	final private DiscreteLocation location;

	public DefaultDiscretizationResult(DiscreteLocation location) {
		this.location = location;
	}

	@Override
	public DiscreteLocation getLocation() {
		return location;
	}
}
