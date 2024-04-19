package org.eqasim.core.simulation.termination.mode_share;

import org.eqasim.core.simulation.termination.EqasimTerminationConfigGroup;
import org.eqasim.core.simulation.termination.EqasimTerminationModule;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ModeShareModule extends AbstractModule {
	@Override
	public void install() {
		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(getConfig());

		for (String mode : terminationConfig.getModes()) {
			EqasimTerminationModule.bindTerminationIndicator(binder(), ModeShareIndicator.PREFIX + mode)
					.toProvider(ModeShareIndicator.createProvider(mode));

			EqasimTerminationModule.bindTerminationCriterion(binder(), ModeShareCriterion.PREFIX + mode)
					.toProvider(ModeShareCriterion.createProvider(mode, terminationConfig.getHorizon(),
							terminationConfig.getSmoothing(), terminationConfig.getThreshold()));
		}

		addEventHandlerBinding().to(ModeShareTracker.class);
		addControlerListenerBinding().to(ModeShareTracker.class);
	}

	@Provides
	@Singleton
	ModeShareTracker provideModeShareTracker() {
		EqasimTerminationConfigGroup terminationConfig = EqasimTerminationConfigGroup.getOrCreate(getConfig());
		return new ModeShareTracker(terminationConfig.getModes());
	}
}
