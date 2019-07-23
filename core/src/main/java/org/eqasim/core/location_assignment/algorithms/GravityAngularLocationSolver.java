package org.eqasim.core.location_assignment.algorithms;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.algorithms.angular.AngularTailProblem;
import org.eqasim.core.location_assignment.algorithms.angular.AngularTailResult;
import org.eqasim.core.location_assignment.algorithms.angular.AngularTailSolver;
import org.eqasim.core.location_assignment.algorithms.gravity.GravityChainProblem;
import org.eqasim.core.location_assignment.algorithms.gravity.GravityChainResult;
import org.eqasim.core.location_assignment.algorithms.gravity.GravityChainSolver;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.relaxation.DefaultRelaxedLocationResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationSolver;

public class GravityAngularLocationSolver implements RelaxedLocationSolver {
	final private GravityChainSolver gravitySolver;
	final private AngularTailSolver angularSolver;

	public GravityAngularLocationSolver(GravityChainSolver gravityChainSolver, AngularTailSolver angularTailSolver) {
		this.gravitySolver = gravityChainSolver;
		this.angularSolver = angularTailSolver;
	}

	@Override
	public RelaxedLocationResult solve(LocationAssignmentProblem problem,
			FeasibleDistanceResult feasibleDistanceResult) {
		if (!problem.getOriginLocation().isPresent() && !problem.getDestinationLocation().isPresent()) {
			throw new IllegalArgumentException("GravityAngularLocationSolver needs at least one fixed location");
		}

		if (problem.getOriginLocation().isPresent() && problem.getDestinationLocation().isPresent()) {
			GravityChainProblem gravityProblem = new GravityChainProblem(problem.getOriginLocation().get(),
					problem.getDestinationLocation().get(), feasibleDistanceResult.getTargetDistances());
			GravityChainResult gravityResult = gravitySolver.solve(gravityProblem);

			return new DefaultRelaxedLocationResult(gravityResult.isConverged(), gravityResult.getIterations(),
					gravityResult.getLocations());
		} else {
			List<Double> orderedTargetDistances = new LinkedList<>(feasibleDistanceResult.getTargetDistances());
			Vector2D anchorLocation = problem.getOriginLocation().isPresent() ? problem.getOriginLocation().get()
					: problem.getDestinationLocation().get();

			if (problem.getDestinationLocation().isPresent()) {
				Collections.reverse(orderedTargetDistances);
			}

			AngularTailProblem angularProblem = new AngularTailProblem(anchorLocation, orderedTargetDistances);
			AngularTailResult angularResult = angularSolver.solve(angularProblem);

			List<Vector2D> relaxedLocations = new LinkedList<>(angularResult.getLocations());

			if (problem.getDestinationLocation().isPresent()) {
				Collections.reverse(relaxedLocations);
			}

			return new DefaultRelaxedLocationResult(angularResult.isConverged(), 0, relaxedLocations);
		}
	}
}
