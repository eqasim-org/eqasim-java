package org.eqasim.core.location_assignment.algorithms.angular;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class AngularTailSolver {
	final private Random random;

	public AngularTailSolver(Random random) {
		this.random = random;
	}

	public AngularTailResult solve(AngularTailProblem problem) {
		List<Vector2D> locations = new LinkedList<>();
		Vector2D currentLocation = problem.getAnchorLocation();

		for (Double distance : problem.getTargetDistances()) {
			double randomAngle = random.nextDouble() * 2.0 * Math.PI;
			Vector2D randomDirection = new Vector2D(Math.cos(randomAngle), Math.sin(randomAngle));
			currentLocation = currentLocation.add(randomDirection.scalarMultiply(distance));
			locations.add(currentLocation);
		}

		return new AngularTailResult(locations);
	}
}
