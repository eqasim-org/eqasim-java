package org.eqasim.core.simulation.termination;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.core.controler.TerminationCriterion;

import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;

public class EqasimTerminationCriterion implements TerminationCriterion {
	private final Map<String, TerminationIndicatorSupplier> indicators;
	private final Map<String, TerminationCriterionCalculator> criteria;

	private final List<TerminationData> history = new LinkedList<>();

	private final int firstIteration;
	private final int lastIteration;

	private final TerminationWriter writer;

	public EqasimTerminationCriterion(int firstIteration, int lastIteration,
			Map<String, TerminationIndicatorSupplier> indicators, Map<String, TerminationCriterionCalculator> criteria,
			TerminationWriter writer) {
		this.firstIteration = firstIteration;
		this.lastIteration = lastIteration;

		this.indicators = indicators;
		this.criteria = criteria;

		this.writer = writer;
	}

	@Override
	public boolean mayTerminateAfterIteration(int iteration) {
		// called at the beginning of an iteration to check if we may terminate now
		// this happens before IterationStartsEvent

		boolean mayTerminate = false;

		if (iteration > firstIteration) {
			// check if we may terminate
			TerminationData terminationData = prepareTerminationData(iteration);
			history.add(terminationData);
			writer.write(history);

			mayTerminate = terminationData.criteria.values().stream().mapToDouble(d -> d).sum() == 0.0;
		}

		if (iteration >= lastIteration) {
			mayTerminate = true;
		}

		return mayTerminate;
	}

	@Override
	public boolean doTerminate(int iteration) {
		// called at the very end of an iteration *if* we said that we might terminate
		// if it is called, this happens after IterationEndsEvent

		// obtain data for the current iteration
		TerminationData terminationData = prepareTerminationData(iteration);
		boolean doTerminate = terminationData.criteria.values().stream().mapToDouble(d -> d).sum() == 0.0;

		if (iteration >= lastIteration) {
			doTerminate = true;
		}

		if (doTerminate) {
			// add information for last iteration and write it (since we won't call
			// mayTerminate again)
			history.add(terminationData);
			writer.write(history);
		}

		return doTerminate;
	}

	private TerminationData prepareTerminationData(int iteration) {
		// obtain indicator values
		ImmutableMap.Builder<String, Double> indicatorValues = ImmutableMap.builder();

		for (var item : indicators.entrySet()) {
			indicatorValues.put(item.getKey(), item.getValue().getValue());
		}

		IterationData iterationData = new IterationData(iteration - 1, indicatorValues.build());

		// obtain criterion values
		ImmutableMap.Builder<String, Double> criterionValues = ImmutableMap.builder();

		for (var item : criteria.entrySet()) {
			criterionValues.put(item.getKey(), item.getValue().calculate(history, iterationData));
		}

		TerminationData terminationData = new TerminationData(iteration - 1, iterationData.indicators,
				criterionValues.build());

		return terminationData;
	}

	public void replay(List<TerminationData> replayData) {
		for (int k = 0; k < replayData.size(); k++) {
			TerminationData item = replayData.get(k);

			if (item.iteration == firstIteration) {
				Verify.verify(k == replayData.size() - 1,
						"Replay data should contain iterations until (inclusive) firstIteration");
				return;
			}
		}

		throw new IllegalStateException("Did not find iteration " + firstIteration + " in replay data");
	}
}
