package org.eqasim.ile_de_france;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eqasim.core.simulation.convergence.ConvergenceSignal;
import org.eqasim.core.simulation.convergence.ConvergenceTerminationCriterion;
import org.eqasim.core.simulation.convergence.criterion.ConvergenceCriterion;
import org.eqasim.core.simulation.convergence.criterion.DerivativeCriterion;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ModeShareListener implements PersonDepartureEventHandler, IterationStartsListener, IterationEndsListener {
	private final List<String> modes = Arrays.asList("car", "pt", "bike", "walk");

	private final Map<String, ConvergenceSignal> signals = new HashMap<>();
	private final Map<String, Integer> tripCount = new HashMap<>();

	@Inject
	public ModeShareListener(OutputDirectoryHierarchy outputHierarchy,
			ConvergenceTerminationCriterion terminationCriterion) {
		ConvergenceCriterion criterion = new DerivativeCriterion(outputHierarchy, //
				20, // Smoothing
				10, // Horizon
				0.01 * 0.01 * 0.5, // First derivative threshold
				0.01 * 0.01 * 0.5 // * 0.01 // Direction change threshold
		);

		for (String mode : modes) {
			ConvergenceSignal signal = new ConvergenceSignal(mode);
			signals.put(mode, signal);

			terminationCriterion.addCriterion(signal, criterion);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		for (String mode : modes) {
			tripCount.put(mode, 0);
		}
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		int total = tripCount.values().stream().mapToInt(v -> v).sum();

		for (String mode : modes) {
			double share = (double) tripCount.get(mode) / total;
			signals.get(mode).addValue(event.getIteration(), share);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (modes.contains(event.getLegMode())) {
			tripCount.compute(event.getLegMode(), (k, v) -> v == null ? 1 : v + 1);
		}
	}
}
