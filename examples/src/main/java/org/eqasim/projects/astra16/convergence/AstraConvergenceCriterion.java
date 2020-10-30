package org.eqasim.projects.astra16.convergence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.analysis.AstraConvergence;
import org.matsim.core.controler.TerminationCriterion;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.inject.Singleton;

@Singleton
public class AstraConvergenceCriterion implements IterationEndsListener, TerminationCriterion {
	private final Map<String, Thresholds> thresholds = new HashMap<>();
	private final Map<String, List<Double>> values = new HashMap<>();

	static public boolean IS_CONVERGED = false;

	public AstraConvergenceCriterion() {
		values.put("waitingTime", new LinkedList<>());
		values.put("travelTime", new LinkedList<>());
		values.put("price", new LinkedList<>());
		values.put("trips", new LinkedList<>());

		thresholds.put("waitingTime",
				new Thresholds(15.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
		thresholds.put("travelTime",
				new Thresholds(60.0, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
		thresholds.put("price", new Thresholds(0.01, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0.01));
		thresholds.put("trips", new Thresholds(Double.POSITIVE_INFINITY, 0.01, Double.POSITIVE_INFINITY, 0.01));
	}

	private void addValue(String slot, double value) {
		values.get(slot).add(value);
	}

	public void addWaitingTimeError(double value) {
		addValue("waitingTime", value);
	}

	public void addTravelTimeError(double value) {
		addValue("travelTime", value);
	}

	public void addPrice(double value) {
		addValue("price", value);
	}

	public void addNumberOfTrips(double value) {
		addValue("trips", value);
	}

	private double calculateMean(List<Double> values, int horizon) {
		double mean = 0.0;

		if (values.size() < horizon) {
			return Double.POSITIVE_INFINITY;
		}

		for (int i = values.size() - horizon; i < values.size(); i++) {
			mean += values.get(i);
		}

		return mean / horizon;
	}

	private State calculateState(List<Double> values, int horizon) {
		State state = new State();

		if (values.size() >= horizon) {
			double mean = calculateMean(values, horizon);
			double firstValue = values.get(values.size() - horizon);
			double lastValue = values.get(values.size() - 1);

			state.absoluteDifference = Math.abs(lastValue - mean);
			state.relativeDifference = Math.abs((lastValue - mean) / mean);

			state.absoluteChange = Math.abs(lastValue - firstValue) / horizon;
			state.relativeChange = Math.abs((lastValue - firstValue) / firstValue) / horizon;
		}

		return state;
	}

	private boolean calculateConvergence() {
		List<Integer> horizons = Arrays.asList(2, 25, 50, 100);
		List<String> slots = Arrays.asList("waitingTime", "travelTime", "price", "trips");

		boolean converged = true;

		for (String slot : slots) {
			boolean slotConverged = true;
			Thresholds thresholds = this.thresholds.get(slot);

			for (int horizon : horizons) {
				State state = calculateState(values.get(slot), horizon);

				boolean horizonConverged = true;
				horizonConverged &= state.absoluteDifference <= thresholds.absoluteDifference;
				horizonConverged &= state.relativeDifference <= thresholds.relativeDifference;
				horizonConverged &= state.absoluteChange <= thresholds.absoluteChange;
				horizonConverged &= state.relativeChange <= thresholds.relativeChange;

				System.out.println(String.format("[Convergence] %s, H: %d, %s [%s]", slot, horizon, state,
						horizonConverged ? "yes" : "no"));

				slotConverged &= horizonConverged;
			}

			System.out.println(String.format("[Convergence] %s [%s]", slot, slotConverged ? "yes" : "no"));

			converged &= slotConverged;
		}

		System.out.println(String.format("[Convergence] [%s]", converged ? "yes" : "no"));
		return converged;
	}

	private class Thresholds {
		double absoluteDifference;
		double relativeDifference;
		double absoluteChange;
		double relativeChange;

		Thresholds(double absoluteDifference, double relativeDifference, double absoluteChange, double relativeChange) {
			this.absoluteDifference = absoluteDifference;
			this.relativeDifference = relativeDifference;
			this.absoluteChange = absoluteChange;
			this.relativeChange = relativeChange;
		}
	}

	private class State {
		double absoluteDifference = Double.POSITIVE_INFINITY;
		double relativeDifference = Double.POSITIVE_INFINITY;

		double absoluteChange = Double.POSITIVE_INFINITY;
		double relativeChange = Double.POSITIVE_INFINITY;

		@Override
		public String toString() {
			return String.format("AD: %f, RD: %f, AC: %f, RC: %f", absoluteDifference, relativeDifference,
					absoluteChange, relativeChange);
		}
	}

	private int triggerIteration = -1000;

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		System.out.println("[Convergence] Iteration " + event.getIteration());
		boolean isConverged = calculateConvergence();

		if (isConverged) {
			AstraConvergence.IS_CONVERGED = true;
			triggerIteration = event.getIteration() + 2;
		}
	}

	@Override
	public boolean continueIterations(int iteration) {
		if (iteration == triggerIteration) {
			return false;
		}

		return true;
	}
}
