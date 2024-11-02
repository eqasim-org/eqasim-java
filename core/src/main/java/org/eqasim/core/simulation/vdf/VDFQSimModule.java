package org.eqasim.core.simulation.vdf;

import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.eqasim.core.simulation.vdf.travel_time.VDFLinkSpeedCalculator;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(EqasimLinkSpeedCalculator.class).to(VDFLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public VDFLinkSpeedCalculator provideVDFLinkSpeedCalculator(Population population, VDFTravelTime travelTime) {
		return new VDFLinkSpeedCalculator(population, travelTime);
	}
}
