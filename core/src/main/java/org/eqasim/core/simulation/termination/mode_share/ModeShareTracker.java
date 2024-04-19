package org.eqasim.core.simulation.termination.mode_share;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import com.google.common.util.concurrent.AtomicDouble;

public class ModeShareTracker implements PersonDepartureEventHandler, IterationEndsListener {
	private final List<String> modes;

	private final Map<String, Double> shares = new HashMap<>();
	private final Map<String, AtomicDouble> counts = new HashMap<>();

	ModeShareTracker(List<String> modes) {
		this.modes = modes;

		for (String mode : modes) {
			counts.put(mode, new AtomicDouble(0.0));
			shares.put(mode, 0.0);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		AtomicDouble value = counts.get(event.getRoutingMode());

		if (value != null) {
			value.addAndGet(1.0);
		}
	}

	public double getModeShare(String mode) {
		return shares.get(mode);
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		double total = counts.values().stream().mapToDouble(AtomicDouble::get).sum();

		for (String mode : modes) {
			shares.put(mode, counts.get(mode).get() / total);
		}

		counts.values().forEach(v -> v.set(0.0));
	}
}
