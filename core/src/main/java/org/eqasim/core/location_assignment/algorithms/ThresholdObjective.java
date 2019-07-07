package org.eqasim.core.location_assignment.algorithms;

import java.util.List;

import org.eqasim.core.location_assignment.assignment.objective.LocationAssignmentObjective;

public class ThresholdObjective implements LocationAssignmentObjective {
	final private boolean isConverged;
	final private double value;

	final private List<Double> errors;
	final private List<Double> absoluteErrors;
	final private List<Double> excessErrors;

	public ThresholdObjective(boolean isConverged, double value, List<Double> errors, List<Double> absoluteErrors,
			List<Double> excessErrors) {
		this.isConverged = isConverged;
		this.value = value;
		this.errors = errors;
		this.absoluteErrors = absoluteErrors;
		this.excessErrors = excessErrors;
	}

	@Override
	public boolean isConverged() {
		return isConverged;
	}

	@Override
	public double getValue() {
		return value;
	}
	
	public List<Double> getErrors() {
		return errors;
	}

	public List<Double> getAbsoluteErrors() {
		return absoluteErrors;
	}

	public List<Double> getExcessErrors() {
		return excessErrors;
	}
}
