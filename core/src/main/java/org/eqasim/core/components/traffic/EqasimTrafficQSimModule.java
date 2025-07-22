package org.eqasim.core.components.traffic;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		addLinkSpeedCalculatorBinding().to(EqasimLinkSpeedCalculator.class);
		bind(EqasimLinkSpeedCalculator.class).to(DefaultEqasimLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public DefaultEqasimLinkSpeedCalculator provideDefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
		return new DefaultEqasimLinkSpeedCalculator(crossingPenalty);
	}
}
