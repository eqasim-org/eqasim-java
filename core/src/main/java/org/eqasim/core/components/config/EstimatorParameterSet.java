package org.eqasim.core.components.config;

import org.matsim.core.config.ReflectiveConfigGroup;

class EstimatorParameterSet extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "estimator";

	static public final String MODE = "mode";
	static public final String ESTIMATOR = "estimator";

	private String mode;
	private String estimator;

	public EstimatorParameterSet() {
		super(GROUP_NAME);
	}

	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	@StringGetter(ESTIMATOR)
	public String getEstimator() {
		return estimator;
	}

	@StringSetter(ESTIMATOR)
	public void setEstimator(String estimator) {
		this.estimator = estimator;
	}
}
