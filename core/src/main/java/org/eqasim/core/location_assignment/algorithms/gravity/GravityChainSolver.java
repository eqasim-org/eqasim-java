package org.eqasim.core.location_assignment.algorithms.gravity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.algorithms.gravity.initial.GravityInitialLocationGenerator;
import org.eqasim.core.location_assignment.algorithms.gravity.initial.LateralDeviationGenerator;

public class GravityChainSolver {
	final private long maximumIterations;
	final private double convergenceThreshold;

	final private double gainFactor;
	final private Random random;
	final private GravityInitialLocationGenerator initialLocationGenerator;
	final private LateralDeviationGenerator zeroDeviationGenerator = new LateralDeviationGenerator(new Random(0), 0.0);

	public GravityChainSolver(double gainFactor, long maximumIterations, double convergenceThreshold, Random random,
			GravityInitialLocationGenerator initialLocationGenerator) {
		this.gainFactor = gainFactor;
		this.maximumIterations = maximumIterations;
		this.convergenceThreshold = convergenceThreshold;
		this.random = random;
		this.initialLocationGenerator = initialLocationGenerator;
	}

	public GravityChainResult solve(GravityChainProblem problem) {
		int numberOfTrips = problem.getTargetDistances().size();

		if (numberOfTrips < 2) {
			throw new IllegalStateException("Not possible to relax a chain with one trip");
		}

		double totalDistance = problem.getTargetDistances().stream().reduce(0.0, Double::sum);
		double directDistance = problem.getOriginLocation().distance(problem.getDestinationLocation());

		if (totalDistance == directDistance) {
			return solveExactCase(problem, directDistance, numberOfTrips);
		} else if (numberOfTrips == 2) {
			return solveSingleLocationCase(problem, directDistance, numberOfTrips);
		} else { // numberOfTrips > 2
			return solveMultiLocationCase(problem, directDistance, numberOfTrips);
		}
	}

	private GravityChainResult solveMultiLocationCase(GravityChainProblem problem, double directDistance,
			int numberOfTrips) {
		double totalDistance = problem.getTargetDistances().stream().reduce(0.0, Double::sum);

		if (totalDistance <= directDistance) {
			List<Vector2D> locations = zeroDeviationGenerator.generate(numberOfTrips - 1, problem.getOriginLocation(),
					problem.getDestinationLocation());
			locations.add(0, problem.getOriginLocation());
			locations.add(problem.getDestinationLocation());

			List<Double> distances = computeDistances(problem, locations.subList(1, numberOfTrips), numberOfTrips);
			List<Double> differences = computeDifferences(problem, distances, numberOfTrips);

			return new GravityChainResult(false, isConverged(problem, differences, numberOfTrips),
					locations.subList(1, numberOfTrips), 0);
		}

		List<Vector2D> stateLocations = new ArrayList<>(numberOfTrips - 1);
		stateLocations.add(problem.getOriginLocation());
		stateLocations.addAll(initialLocationGenerator.generate(numberOfTrips - 1, problem.getOriginLocation(),
				problem.getDestinationLocation()));
		stateLocations.add(problem.getDestinationLocation());

		int iteration = 0;
		boolean isConverged = false;

		while (iteration < maximumIterations && !isConverged) {
			List<Vector2D> newLocations = new ArrayList<>(numberOfTrips + 1);
			newLocations.add(problem.getOriginLocation());

			for (int index = 1; index < numberOfTrips; index++) {
				Vector2D currentLocation = stateLocations.get(index);
				Vector2D previousLocation = stateLocations.get(index - 1);
				Vector2D nextLocation = stateLocations.get(index + 1);

				double expectedPreviousDistance = problem.getTargetDistances().get(index - 1);
				double expectedNextDistance = problem.getTargetDistances().get(index);

				double previousDistance = previousLocation.distance(currentLocation);
				double nextDistance = nextLocation.distance(currentLocation);

				double previousDifference = expectedPreviousDistance - previousDistance;
				double nextDifference = expectedNextDistance - nextDistance;

				Vector2D previousDirection = previousLocation.subtract(currentLocation);
				Vector2D nextDirection = nextLocation.subtract(currentLocation);

				if (previousDirection.getNorm() > 0.0) {
					previousDirection = previousDirection.normalize();
				} else {
					previousDirection = new Vector2D(1.0, 0.0);
				}

				if (nextDirection.getNorm() > 0.0) {
					nextDirection = nextDirection.normalize();
				} else {
					nextDirection = new Vector2D(1.0, 0.0);
				}

				Vector2D updateFromPrevious = previousDirection.scalarMultiply(-gainFactor * previousDifference);
				Vector2D updateFromNext = nextDirection.scalarMultiply(-gainFactor * nextDifference);

				Vector2D update = updateFromPrevious.scalarMultiply(0.5).add(updateFromNext.scalarMultiply(0.5));
				newLocations.add(currentLocation.add(update));
			}

			newLocations.add(problem.getDestinationLocation());
			stateLocations = newLocations;

			List<Double> distances = computeDistances(problem, stateLocations.subList(1, numberOfTrips), numberOfTrips);
			List<Double> differences = computeDifferences(problem, distances, numberOfTrips);
			isConverged = isConverged(problem, differences, numberOfTrips);
			iteration++;
		}
		
		return new GravityChainResult(true, isConverged, stateLocations.subList(1, numberOfTrips), iteration);
	}

	private GravityChainResult solveSingleLocationCase(GravityChainProblem problem, double directDistance,
			int numberOfTrips) {
		double originDistance = problem.getTargetDistances().get(0);
		double destinationDistance = problem.getTargetDistances().get(1);

		boolean isFeasible = true;
		Vector2D location = null;
		
		if (directDistance == 0.0) {
			double alpha = random.nextDouble();
			double radius = 0.5 * originDistance + 0.5 * destinationDistance;
			Vector2D offset = new Vector2D(Math.cos(alpha), Math.sin(alpha)).scalarMultiply(radius);
			location = problem.getOriginLocation().add(offset);
			isFeasible = originDistance == destinationDistance;
		} else if (directDistance > originDistance + destinationDistance) {
			double ratio = 1.0;
			
			if (originDistance > 0.0 || destinationDistance > 0.0) {
				ratio = originDistance / (originDistance + destinationDistance);
			}
			
			Vector2D direction = problem.getDestinationLocation().subtract(problem.getOriginLocation()).normalize();

			location = problem.getOriginLocation().add(direction.scalarMultiply(ratio * directDistance));
			isFeasible = false;
		} else if (directDistance < Math.abs(originDistance - destinationDistance)) {
			double ratio = 1.0;
			
			if (originDistance > 0.0 || destinationDistance > 0.0) {
				ratio = originDistance / (originDistance + destinationDistance);
			}
			
			double maximumDistance = Math.max(originDistance, destinationDistance);
			Vector2D direction = problem.getDestinationLocation().subtract(problem.getOriginLocation()).normalize();

			location = problem.getOriginLocation().add(direction.scalarMultiply(ratio * maximumDistance));
			isFeasible = false;
		} else {
			Vector2D direction = problem.getDestinationLocation().subtract(problem.getOriginLocation()).normalize();
			double A = 0.5 * (Math.pow(originDistance, 2.0) - Math.pow(destinationDistance, 2.0)
					+ Math.pow(directDistance, 2.0)) / directDistance;

			// The math.max here is added to solve numerical problems (negative root)
			double H = Math.sqrt(Math.max(0.0, Math.pow(originDistance, 2.0) - Math.pow(A, 2.0)));

			double r = random.nextDouble();

			Vector2D center = problem.getOriginLocation().add(direction.scalarMultiply(A));
			Vector2D offset = direction.scalarMultiply(H);
			offset = new Vector2D(-offset.getY(), offset.getX());

			location = (r < 0.5) ? center.add(offset) : center.add(-1.0, offset);
		}

		List<Double> distances = computeDistances(problem, Collections.singletonList(location), numberOfTrips);
		List<Double> differences = computeDifferences(problem, distances, numberOfTrips);
		boolean isConverged = isConverged(problem, differences, numberOfTrips);
		
		return new GravityChainResult(isFeasible, isConverged, Collections.singletonList(location), 0);
	}

	private GravityChainResult solveExactCase(GravityChainProblem problem, double directDistance, int numberOfTrips) {
		if (directDistance == 0.0) { // Special case: We cannot get a direction vector in that case
			return new GravityChainResult(
					true, true, IntStream.range(0, numberOfTrips - 1)
							.mapToObj(i -> new Vector2D(1.0, problem.getOriginLocation())).collect(Collectors.toList()),
					0);
		}

		Vector2D direction = problem.getDestinationLocation().subtract(problem.getOriginLocation()).normalize();
		List<Vector2D> locations = new LinkedList<>();

		double currentDistance = 0.0;

		for (Double tripDistance : problem.getTargetDistances().subList(0, numberOfTrips - 1)) {
			currentDistance += tripDistance;
			locations.add(problem.getOriginLocation().add(direction.scalarMultiply(currentDistance)));
		}

		return new GravityChainResult(true, true, locations, 0);
	}

	private List<Double> computeDistances(GravityChainProblem problem, List<Vector2D> locations, int numberOfTrips) {
		if (numberOfTrips > 1) {
			List<Double> distances = new LinkedList<>();

			distances.add(problem.getOriginLocation().distance(locations.get(0)));

			for (int i = 0; i < numberOfTrips - 2; i++) {
				distances.add(locations.get(i).distance(locations.get(i + 1)));
			}

			distances.add(problem.getDestinationLocation().distance(locations.get(numberOfTrips - 2)));

			return distances;
		} else if (numberOfTrips == 1) {
			return Collections.singletonList(problem.getOriginLocation().distance(problem.getDestinationLocation()));
		} else {
			return Collections.emptyList();
		}
	}

	private List<Double> computeDifferences(GravityChainProblem problem, List<Double> distances, int numberOfTrips) {
		List<Double> differences = new LinkedList<>();

		for (int i = 0; i < numberOfTrips; i++) {
			differences.add(problem.getTargetDistances().get(i) - distances.get(i));
		}

		return differences;
	}

	private boolean isConverged(GravityChainProblem problem, List<Double> differences, int numberOfTrips) {
		boolean isConverged = true;

		for (int i = 0; i < numberOfTrips; i++) {
			isConverged &= Math.abs(differences.get(i)) <= convergenceThreshold;
		}

		return isConverged;
	}
}
