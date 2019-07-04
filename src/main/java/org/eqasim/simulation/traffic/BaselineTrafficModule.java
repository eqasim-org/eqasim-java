package org.eqasim.simulation.traffic;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.DefaultLinkSpeedCalculator;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class BaselineTrafficModule extends AbstractModule {
	final private double crossingPenalty;

	public BaselineTrafficModule(double crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public void install() {
	}

	@Provides
	@Singleton
	public QNetworkFactory provideQNetworkFactory(EventsManager events, Scenario scenario,
			BaselineLinkSpeedCalculator linkSpeedCalculator) {
		ConfigurableQNetworkFactory networkFactory = new ConfigurableQNetworkFactory(events, scenario);
		networkFactory.setLinkSpeedCalculator(linkSpeedCalculator);
		return networkFactory;
	}

	@Provides
	@Singleton
	public BaselineLinkSpeedCalculator provideBaselineLinkSpeedCalculator() {
		DefaultLinkSpeedCalculator delegate = new DefaultLinkSpeedCalculator();
		return new BaselineLinkSpeedCalculator(delegate, crossingPenalty);
	}
}
