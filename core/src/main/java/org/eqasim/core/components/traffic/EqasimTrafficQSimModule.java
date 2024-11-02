package org.eqasim.core.components.traffic;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		addLinkSpeedCalculator().to(EqasimLinkSpeedCalculator.class);
		bind(EqasimLinkSpeedCalculator.class).to(DefaultEqasimLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public DefaultEqasimLinkSpeedCalculator provideDefaultEqasimLinkSpeedCalculator(EqasimConfigGroup eqasimConfig) {
		return new DefaultEqasimLinkSpeedCalculator(eqasimConfig.getCrossingPenalty());
	}
}
