package org.eqasim.core.components.traffic;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(LinkSpeedCalculator.class).to(EqasimLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public EqasimLinkSpeedCalculator provideBaselineLinkSpeedCalculator(EqasimConfigGroup eqasimConfig) {
		return new EqasimLinkSpeedCalculator(eqasimConfig.getCrossingPenalty());
	}
}
