package org.eqasim.core.simulation.mode_choice.utilities.estimators;

public class EstimatorUtilities {
	private EstimatorUtilities() {
	}

	private final static double DEFAULT_MINIMUM_INTERACTION_VALUE = 1e-3;

	static public double interaction(double value, double referenceValue, double exponent, double minimumValue) {
		return Math.pow(Math.max(value, minimumValue) / referenceValue, exponent);
	}

	static public double interaction(double value, double referenceValue, double exponent) {
		return interaction(value, referenceValue, exponent, DEFAULT_MINIMUM_INTERACTION_VALUE);
	}
}
