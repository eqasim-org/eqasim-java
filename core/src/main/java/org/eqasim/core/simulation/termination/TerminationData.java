package org.eqasim.core.simulation.termination;

import com.google.common.collect.ImmutableMap;

public class TerminationData extends IterationData {
	public final ImmutableMap<String, Double> criteria;

	public TerminationData(int iteration, ImmutableMap<String, Double> indicators,
			ImmutableMap<String, Double> criteria) {
		super(iteration, indicators);
		this.criteria = criteria;
	}
}
