package org.eqasim.core.components.traffic;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		addLinkSpeedCalculator().to(EqasimLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public EqasimLinkSpeedCalculator provideBaselineLinkSpeedCalculator(CrossingPenalty crossigPenalty) {
		return new EqasimLinkSpeedCalculator(crossigPenalty);
	}
}
