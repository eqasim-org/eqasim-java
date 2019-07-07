package org.eqasim.core.location_assignment.matsim.discretizer;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.algorithms.discretizer.DefaultDiscretizationResult;
import org.eqasim.core.location_assignment.algorithms.discretizer.Discretizer;
import org.eqasim.core.location_assignment.algorithms.discretizer.DiscretizerResult;
import org.matsim.core.utils.collections.QuadTree;

public class FacilityDiscretizer implements Discretizer {
	final private QuadTree<FacilityLocation> candidates;

	public FacilityDiscretizer(QuadTree<FacilityLocation> candidates) {
		this.candidates = candidates;
	}

	@Override
	public DiscretizerResult discretize(Vector2D location) {
		return new DefaultDiscretizationResult(candidates.getClosest(location.getX(), location.getY()));
	}
}
