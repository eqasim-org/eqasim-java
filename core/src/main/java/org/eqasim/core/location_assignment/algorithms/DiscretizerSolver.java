package org.eqasim.core.location_assignment.algorithms;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.location_assignment.algorithms.discretizer.Discretizer;
import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocation;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationResult;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocationSolver;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.relaxation.RelaxedLocationResult;

public class DiscretizerSolver implements DiscreteLocationSolver {
	final private DiscretizerProvider discretizerProvider;

	public DiscretizerSolver(DiscretizerProvider discretizerProvider) {
		this.discretizerProvider = discretizerProvider;
	}

	@Override
	public DiscreteLocationResult solve(LocationAssignmentProblem problem,
			FeasibleDistanceResult feasibleDistanceResult, RelaxedLocationResult relaxedLocationResult) {
		List<Discretizer> discretizers = discretizerProvider.getDiscretizers(problem);
		List<DiscreteLocation> discreteLocations = new LinkedList<>();

		for (int i = 0; i < discretizers.size(); i++) {
			discreteLocations.add(
					discretizers.get(i).discretize(relaxedLocationResult.getRelaxedLocations().get(i)).getLocation());
		}

		return new DiscreteLocationResult() {
			@Override
			public List<DiscreteLocation> getDiscreteLocations() {
				return discreteLocations;
			}
		};
	}

	public interface DiscretizerProvider {
		List<Discretizer> getDiscretizers(LocationAssignmentProblem problem);
	}
}
