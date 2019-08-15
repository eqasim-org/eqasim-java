package org.eqasim.core.simulation.mode_choice.utilities.estimators;

public class EstimatorUtils {
	private EstimatorUtils() {
	}

	public static final double DEFAULT_MINIMUM_VALUE = 1e-3;

	public static double interaction(double value, double reference, double exponent, double minimumValue) {
		return Math.pow(Math.max(minimumValue, value) / reference, exponent);
	}

	public static double interaction(double value, double reference, double exponent) {
		return interaction(value, reference, exponent, DEFAULT_MINIMUM_VALUE);
	}
}
