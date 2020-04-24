package org.eqasim.core.components.traffic;

import org.eqasim.core.components.config.EqasimConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimTrafficQSimTrafficLightsModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {

	}

	@Provides
	@Singleton
	public QNetworkFactory provideQNetworkFactory(EventsManager events, Scenario scenario,
			EqasimLinkSpeedCalculatorTrafficLights linkSpeedCalculator) {
		ConfigurableQNetworkFactory networkFactory = new ConfigurableQNetworkFactory(events, scenario);
		networkFactory.setLinkSpeedCalculator(linkSpeedCalculator);
		return networkFactory;
	}

	@Provides
	@Singleton
	public EqasimLinkSpeedCalculatorTrafficLights provideBaselineLinkSpeedCalculator(EqasimConfigGroup eqasimConfig) {
		DefaultLinkSpeedCalculator delegate = new DefaultLinkSpeedCalculator();
		return new EqasimLinkSpeedCalculatorTrafficLights(delegate, eqasimConfig.getCrossingPenalty());
	}
}
