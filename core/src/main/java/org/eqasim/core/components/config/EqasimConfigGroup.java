package org.eqasim.core.components.config;

import org.matsim.core.config.ReflectiveConfigGroup;

public class EqasimConfigGroup extends ReflectiveConfigGroup {
	public final static String GROUP_NAME = "eqasim";

	private final static String CROSSING_PENALTY = "crossingPenalty";
	private final static String SAMPLE_SIZE = "sampleSize";

	private double crossingPenalty = 3.0;
	private double sampleSize = 1.0;

	public EqasimConfigGroup() {
		super(GROUP_NAME);
	}

	@StringGetter(CROSSING_PENALTY)
	public double getCrossingPenalty() {
		return crossingPenalty;
	}

	@StringSetter(CROSSING_PENALTY)
	public void setCrossingPenalty(double crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@StringGetter(SAMPLE_SIZE)
	public double getSampleSize() {
		return sampleSize;
	}

	@StringSetter(SAMPLE_SIZE)
	public void setSampleSize(double sampleSize) {
		this.sampleSize = sampleSize;
	}
}
