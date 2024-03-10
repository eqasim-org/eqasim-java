package org.eqasim.core.simulation.termination;

import com.google.common.collect.ImmutableMap;

public class IterationData {
	public final int iteration;
	public final ImmutableMap<String, Double> indicators;

	public IterationData(int iteration, ImmutableMap<String, Double> indicators) {
		this.iteration = iteration;
		this.indicators = indicators;
	}
}
