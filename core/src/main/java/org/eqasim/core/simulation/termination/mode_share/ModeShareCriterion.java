package org.eqasim.core.simulation.termination.mode_share;

import java.util.List;

import org.eqasim.core.simulation.termination.IterationData;
import org.eqasim.core.simulation.termination.TerminationCriterionCalculator;
import org.eqasim.core.simulation.termination.TerminationData;

import com.google.common.base.Preconditions;
import com.google.inject.Provider;

public class ModeShareCriterion implements TerminationCriterionCalculator {
	static public final String PREFIX = "mode_share:";

	private final String mode;
	private final double threshold;

	private final int smoothing;
	private final int horizon;

	public ModeShareCriterion(String mode, int horizon, int smoothing, double threshold) {
		Preconditions.checkArgument(horizon % 2 == 0);
		Preconditions.checkArgument(smoothing % 2 == 0);

		this.mode = mode;
		this.threshold = threshold;
		this.horizon = horizon;
		this.smoothing = smoothing;
	}

	@Override
	public double calculate(List<TerminationData> history, IterationData iteration) {
		/*-
		 * 
		 * |-------------|-----|-----|----------|-----|-----|
		 *            -h-s   -h-s/2  -h         -s   -s/2    0
		 */

		if (history.size() - smoothing - horizon - 1 < 0) {
			return Double.NaN;
		}

		double first = history.subList(history.size() - horizon - smoothing - 1, history.size() - horizon).stream()
				.mapToDouble(item -> item.indicators.get(ModeShareIndicator.PREFIX + mode)).average().getAsDouble();

		double second = history.subList(history.size() - smoothing - 1, history.size()).stream()
				.mapToDouble(item -> item.indicators.get(ModeShareIndicator.PREFIX + mode)).average().getAsDouble();

		double difference = Math.abs(second - first);
		return Math.max(0.0, difference - threshold);
	}

	static public Provider<ModeShareCriterion> createProvider(String mode, int horizon, int smoothing,
			double threshold) {
		return new Provider<>() {
			@Override
			public ModeShareCriterion get() {
				return new ModeShareCriterion(mode, horizon, smoothing, threshold);
			}
		};
	}
}
