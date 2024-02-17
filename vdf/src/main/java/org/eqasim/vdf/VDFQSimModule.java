package org.eqasim.vdf;

import org.eqasim.vdf.travel_time.VDFLinkSpeedCalculator;
import org.eqasim.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.qnetsimengine.ConfigurableQNetworkFactory;
import org.matsim.core.mobsim.qsim.qnetsimengine.QNetworkFactory;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
	}

	@Provides
	@Singleton
	public QNetworkFactory provideQNetworkFactory(EventsManager events, Scenario scenario,
			VDFLinkSpeedCalculator linkSpeedCalculator) {
		ConfigurableQNetworkFactory networkFactory = new ConfigurableQNetworkFactory(events, scenario);
		networkFactory.setLinkSpeedCalculator(linkSpeedCalculator);
		return networkFactory;
	}

	@Provides
	@Singleton
	public VDFLinkSpeedCalculator provideVDFLinkSpeedCalculator(Population population, VDFTravelTime travelTime) {
		return new VDFLinkSpeedCalculator(population, travelTime);
	}
}
