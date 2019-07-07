package org.eqasim.core.location_assignment.algorithms.gravity.initial;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class LateralDeviationGenerator implements GravityInitialLocationGenerator {
	final private Random random;
	final private double std;

	public LateralDeviationGenerator(Random random, double std) {
		this.random = random;
		this.std = std;
	}

	@Override
	public List<Vector2D> generate(int numberOfLocations, Vector2D originLocation, Vector2D destinationLocation) {
		double distance = originLocation.distance(destinationLocation);
		
		Vector2D direction;
		
		if (distance > 0.0) {
			direction = destinationLocation.subtract(originLocation).normalize();
		} else {
			double x = 2.0 * random.nextDouble() - 1.0;
			double y = 2.0 * random.nextDouble() - 1.0;
			direction = new Vector2D(x, y).normalize();
		}
		
		Vector2D normal = new Vector2D(-direction.getY(), direction.getX());

		List<Vector2D> result = new ArrayList<>(numberOfLocations);

		for (int i = 1; i < numberOfLocations + 1; i++) {
			double s = (double) i / (double) (numberOfLocations + 1);

			Vector2D location = originLocation.add(direction.scalarMultiply(s * distance));
			location = location.add(normal.scalarMultiply(random.nextGaussian() * std));

			result.add(location);
		}

		return result;
	}

}
