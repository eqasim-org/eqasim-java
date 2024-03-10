package org.eqasim.core.simulation.termination.mode_share;

import org.eqasim.core.simulation.termination.TerminationIndicatorSupplier;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ModeShareIndicator implements TerminationIndicatorSupplier {
	static public final String PREFIX = "mode_share:";

	private final String mode;
	private final ModeShareTracker tracker;

	ModeShareIndicator(String mode, ModeShareTracker tracker) {
		this.mode = mode;
		this.tracker = tracker;
	}

	@Override
	public double getValue() {
		return tracker.getModeShare(mode);
	}

	static public Provider<TerminationIndicatorSupplier> createProvider(String mode) {
		return new Provider<>() {
			@Inject
			ModeShareTracker tracker;

			@Override
			public TerminationIndicatorSupplier get() {
				return new ModeShareIndicator(mode, tracker);
			}
		};
	}
}
