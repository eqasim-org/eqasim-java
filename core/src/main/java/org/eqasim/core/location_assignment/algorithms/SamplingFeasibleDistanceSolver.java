package org.eqasim.core.location_assignment.algorithms;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceSolver;

public class SamplingFeasibleDistanceSolver implements FeasibleDistanceSolver {
	final private int maximumIterations;
	final private DistanceSamplerProvider distanceSamplerProvider;

	public SamplingFeasibleDistanceSolver(int maximumIterations, DistanceSamplerProvider distanceSamplerProvider) {
		this.maximumIterations = maximumIterations;
		this.distanceSamplerProvider = distanceSamplerProvider;
	}

	@Override
	public FeasibleDistanceResult solve(LocationAssignmentProblem problem) {
		List<DistanceSampler> samplers = distanceSamplerProvider.getDistanceSamplers(problem);

		if (problem.getOriginLocation().isPresent() && problem.getDestinationLocation().isPresent()) {
			double directDistance = problem.getOriginLocation().get().distance(problem.getDestinationLocation().get());

			if (directDistance == 0.0 && samplers.size() == 2) {
				final double sample = samplers.get(0).sample();

				return new FeasibleDistanceResult() {
					@Override
					public boolean isConverged() {
						return true;
					}

					@Override
					public List<Double> getTargetDistances() {
						return Arrays.asList(sample, sample);
					}
				};
			}

			double bestObjective = Double.POSITIVE_INFINITY;
			List<Double> bestDistances = new LinkedList<>();

			int iteration = 0;
			boolean converged = false;

			while (iteration < maximumIterations && !converged) {
				List<Double> distances = samplers.stream().map(DistanceSampler::sample).collect(Collectors.toList());

				double totalDistance = distances.stream().reduce(0.0, Double::sum);
				double objective = directDistance - totalDistance;

				if (objective < bestObjective) {
					bestObjective = objective;
					bestDistances = distances;
				}

				converged = bestObjective <= 0;
				iteration++;
			}

			final boolean finalConverged = converged;
			final List<Double> finalDistances = bestDistances;

			return new FeasibleDistanceResult() {
				@Override
				public boolean isConverged() {
					return finalConverged;
				}

				@Override
				public List<Double> getTargetDistances() {
					return finalDistances;
				}
			};
		} else {
			List<Double> distances = samplers.stream().map(DistanceSampler::sample).collect(Collectors.toList());

			return new FeasibleDistanceResult() {
				@Override
				public boolean isConverged() {
					return true;
				}

				@Override
				public List<Double> getTargetDistances() {
					return distances;
				}
			};
		}
	}

	public interface DistanceSamplerProvider {
		List<DistanceSampler> getDistanceSamplers(LocationAssignmentProblem problem);
	}
}
