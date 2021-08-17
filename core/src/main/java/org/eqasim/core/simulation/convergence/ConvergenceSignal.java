package org.eqasim.core.simulation.convergence;

import java.util.LinkedList;
import java.util.List;

public class ConvergenceSignal {
	private final String name;
	private final List<Integer> iterations = new LinkedList<>();
	private final List<Double> values = new LinkedList<>();

	public ConvergenceSignal(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<Integer> getIterations() {
		return iterations;
	}

	public List<Double> getValues() {
		return values;
	}

	public void addValue(int iteration, double value) {
		this.values.add(value);
		this.iterations.add(iteration);
	}
}
