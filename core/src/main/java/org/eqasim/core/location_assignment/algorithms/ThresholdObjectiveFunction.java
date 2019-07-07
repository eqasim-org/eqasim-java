package org.eqasim.core.location_assignment.algorithms;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocation;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjective;
import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjectiveFunction;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;

public class ThresholdObjectiveFunction implements LocationAssignmentObjectiveFunction {
	final private DiscretizationThresholdProvider thresholdProvider;

	public ThresholdObjectiveFunction(DiscretizationThresholdProvider thresholdProvider) {
		this.thresholdProvider = thresholdProvider;
	}

	@Override
	public LocationAssignmentObjective computeObjective(LocationAssignmentProblem problem,
			FeasibleDistanceResult feasibleDistanceResult, RelaxedLocationResult relaxedResult,
			DiscreteLocationResult discreteResult) {
		List<Double> thresholds = thresholdProvider.getDiscretizationThresholds(problem);

		List<Vector2D> allLocations = new LinkedList<>(discreteResult.getDiscreteLocations().stream()
				.map(DiscreteLocation::getLocation).collect(Collectors.toList()));

		if (problem.getOriginLocation().isPresent()) {
			allLocations.add(0, problem.getOriginLocation().get());
		}

		if (problem.getDestinationLocation().isPresent()) {
			allLocations.add(problem.getDestinationLocation().get());
		}

		List<Double> observedDistances = new LinkedList<>();

		for (int i = 0; i < allLocations.size() - 1; i++) {
			observedDistances.add(allLocations.get(i + 1).distance(allLocations.get(i)));
		}

		List<Double> errors = new LinkedList<>();

		for (int i = 0; i < observedDistances.size(); i++) {
			errors.add(observedDistances.get(i) - feasibleDistanceResult.getTargetDistances().get(i));
		}

		List<Double> absoluteErrors = errors.stream().map(Math::abs).collect(Collectors.toList());

		List<Double> excessErrors = new LinkedList<>();
		boolean converged = true;

		for (int i = 0; i < observedDistances.size(); i++) {
			converged &= absoluteErrors.get(i) <= thresholds.get(i);
			excessErrors.add(Math.max(0.0, absoluteErrors.get(i) - thresholds.get(i)));
		}

		double value = excessErrors.stream().reduce(0.0, Double::max);

		return new ThresholdObjective(converged, value, errors, absoluteErrors, excessErrors);
	}

	public interface DiscretizationThresholdProvider {
		List<Double> getDiscretizationThresholds(LocationAssignmentProblem problem);
	}
}
