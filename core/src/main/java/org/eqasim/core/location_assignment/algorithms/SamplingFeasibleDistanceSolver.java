package org.eqasim.core.location_assignment.algorithms;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eqasim.core.location_assignment.assignment.LocationAssignmentProblem;
import org.eqasim.core.location_assignment.assignment.distance.DefaultFeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceResult;
import org.eqasim.core.location_assignment.assignment.distance.FeasibleDistanceSolver;

public class SamplingFeasibleDistanceSolver implements FeasibleDistanceSolver {
	final private int maximumIterations;
	final private DistanceSamplerProvider distanceSamplerProvider;

	public SamplingFeasibleDistanceSolver(int maximumIterations, DistanceSamplerProvider distanceSamplerProvider) {
		this.maximumIterations = maximumIterations;
		this.distanceSamplerProvider = distanceSamplerProvider;
	}

	private double calculateObjective(List<Double> distances, double directDistance) {
		double deltaDistance = 0.0;

		for (int i = 0; i < distances.size(); i++) {
			double referenceDistance = distances.get(i);
			double remainingDistance = 0.0;

			for (int j = 0; j < distances.size(); j++) {
				if (i != j) {
					remainingDistance += distances.get(j);
				}
			}

			if (referenceDistance > directDistance + remainingDistance) {
				deltaDistance = Math.max(deltaDistance, referenceDistance - directDistance - remainingDistance);
			}
		}

		double totalDistance = distances.stream().reduce(0.0, Double::sum);

		if (totalDistance < directDistance) {
			deltaDistance = Math.max(directDistance - totalDistance, deltaDistance);
		}

		return deltaDistance;
	}

	@Override
	public FeasibleDistanceResult solve(LocationAssignmentProblem problem) {
		List<DistanceSampler> samplers = distanceSamplerProvider.getDistanceSamplers(problem);

		if (problem.getOriginLocation().isPresent() && problem.getDestinationLocation().isPresent()) {
			double directDistance = problem.getOriginLocation().get().distance(problem.getDestinationLocation().get());

			if (directDistance == 0.0 && samplers.size() == 2) {
				double sample = samplers.get(0).sample();
				return new DefaultFeasibleDistanceResult(true, 0, Arrays.asList(sample, sample));
			}

			double bestObjective = Double.POSITIVE_INFINITY;
			List<Double> bestDistances = null;

			int iteration = 0;
			boolean converged = false;

			while (iteration < maximumIterations && !converged) {
				List<Double> distances = samplers.stream().map(DistanceSampler::sample).collect(Collectors.toList());
				double objective = calculateObjective(distances, directDistance);

				if (objective < bestObjective || bestDistances == null) {
					bestObjective = objective;
					bestDistances = distances;
				}

				converged = bestObjective == 0.0;
				iteration++;
			}

			return new DefaultFeasibleDistanceResult(converged, iteration, bestDistances);
		} else {
			List<Double> distances = samplers.stream().map(DistanceSampler::sample).collect(Collectors.toList());
			return new DefaultFeasibleDistanceResult(true, 0, distances);
		}
	}

	public interface DistanceSamplerProvider {
		List<DistanceSampler> getDistanceSamplers(LocationAssignmentProblem problem);
	}
}
