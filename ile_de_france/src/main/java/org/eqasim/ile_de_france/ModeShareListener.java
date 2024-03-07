package org.eqasim.ile_de_france;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

public class ModeShareListener implements PersonDepartureEventHandler, IterationStartsListener, IterationEndsListener {
	private final List<String> modes = Arrays.asList("car", "pt", "bike", "walk", "car_passenger");

	private final Map<String, ConvergenceSignal> signals = new HashMap<>();
	private final Map<String, Integer> tripCount = new HashMap<>();

	private final OutputDirectoryHierarchy outputHierarchy;

	public ModeShareListener(OutputDirectoryHierarchy outputHierarchy,
			ConvergenceTerminationCriterion terminationCriterion, Optional<File> signalInputPath) {
		this.outputHierarchy = outputHierarchy;

		ConvergenceCriterion criterion = new DerivativeCriterion(outputHierarchy, //
				20, // Smoothing
				10, // Horizon
				0.01 * 0.01 * 0.5, // First derivative threshold
				0.01 * 0.01 * 0.5 // * 0.01 // Direction change threshold
		);

		for (String mode : modes) {
			ConvergenceSignal signal = new ConvergenceSignal(mode);

			if (signalInputPath.isPresent()) {
				signal.read(new File(signalInputPath.get(), "signal_" + mode + ".csv"));
			}

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
			signals.get(mode).write(new File(outputHierarchy.getOutputFilename("signal_" + mode + ".csv")));
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		String convergenceMode = event.getLegMode();

		if (convergenceMode.startsWith("pt")) {
			convergenceMode = "pt";
		}

		if (modes.contains(convergenceMode)) {
			tripCount.compute(convergenceMode, (k, v) -> v == null ? 1 : v + 1);
		}
	}
}