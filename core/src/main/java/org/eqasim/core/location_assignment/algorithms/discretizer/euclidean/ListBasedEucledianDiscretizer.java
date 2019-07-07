package org.eqasim.core.location_assignment.algorithms.discretizer.euclidean;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.eqasim.core.location_assignment.algorithms.discretizer.DefaultDiscretizationResult;
import org.eqasim.core.location_assignment.algorithms.discretizer.Discretizer;
import org.eqasim.core.location_assignment.algorithms.discretizer.DiscretizerResult;
import org.eqasim.core.location_assignment.assignment.discretization.DiscreteLocation;

public class ListBasedEucledianDiscretizer implements Discretizer {
	final private Set<DiscreteLocation> candidates;
	final private double radius;
	final private long maximumSetSize;
	final private Random random;

	public ListBasedEucledianDiscretizer(Set<DiscreteLocation> candidates, double radius, long maximumSetSize, Random random) {
		this.candidates = candidates;
		this.radius = radius;
		this.maximumSetSize = maximumSetSize;
		this.random = random;
	}

	@Override
	public DiscretizerResult discretize(Vector2D location) {
		List<DiscreteLocation> choiceSet = new LinkedList<>();

		DiscreteLocation fallback = null;
		double fallbackDistance = Double.POSITIVE_INFINITY;

		for (DiscreteLocation candidate : candidates) {
			double distance = candidate.getLocation().distance(location);

			if (distance < fallbackDistance) {
				fallback = candidate;
			}

			if (distance <= radius) {
				choiceSet.add(candidate);
			}

			if (choiceSet.size() == maximumSetSize) {
				break;
			}
		}

		if (choiceSet.size() > 0) {
			return new DefaultDiscretizationResult(choiceSet.get(random.nextInt(choiceSet.size())));
		} else {
			return new DefaultDiscretizationResult(fallback);
		}
	}
}
