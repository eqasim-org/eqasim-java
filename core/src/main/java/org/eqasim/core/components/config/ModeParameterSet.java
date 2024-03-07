package org.eqasim.core.components.config;

import java.util.HashMap;
import java.util.Map;

import org.matsim.core.config.ReflectiveConfigGroup;

public class ModeParameterSet extends ReflectiveConfigGroup {
	static public final String GROUP_NAME = "mode";

	static public final String MODE = "mode";
	static public final String ESTIMATOR = "estimator";
	static public final String USE_PSEUDO_RANDOM_ERROR = "usePseudoRandomError";

	private String mode;
	private String estimator;
	private boolean isUsePseudoRandomError = false;

	public ModeParameterSet() {
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

	@StringGetter(USE_PSEUDO_RANDOM_ERROR)
	public boolean isUsePseudoRandomError() {
		return isUsePseudoRandomError;
	}

	@StringSetter(USE_PSEUDO_RANDOM_ERROR)
	public void setUsePseudoRandomError(boolean value) {
		this.isUsePseudoRandomError = value;
	}

	public Map<String, String> getComments() {
		Map<String, String> comments = new HashMap<>();
		comments.put(MODE, "Mode that will be handled by the estimator");
		comments.put(ESTIMATOR, "Binding name of the estimator");
		comments.put(USE_PSEUDO_RANDOM_ERROR, "Whether to augment the estimator with a pseudo random term (epsilon)");
		return comments;
	}
}
