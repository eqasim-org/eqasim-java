package org.eqasim.examples.corsica_drt.rejections;

import java.util.Collection;
import java.util.Random;

import org.matsim.contribs.discrete_mode_choice.modules.AbstractDiscreteModeChoiceExtension;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class RejectionModule extends AbstractDiscreteModeChoiceExtension {
	private final Collection<String> modes;

	public RejectionModule(Collection<String> modes) {
		this.modes = modes;
	}

	@Override
	protected void installExtension() {
		addEventHandlerBinding().to(RejectionTracker.class);
		bindTripConstraintFactory(RejectionConstraint.NAME).to(RejectionConstraint.Factory.class);
	}

	@Provides
	@Singleton
	public RejectionTracker provideRejectionTracker() {
		return new RejectionTracker();
	}

	@Provides
	@Singleton
	public RejectionConstraint.Factory provideRejectionConstraintFactory(RejectionTracker tracker) {
		Random random = new Random();
		return new RejectionConstraint.Factory(tracker, random, modes);
	}
}
