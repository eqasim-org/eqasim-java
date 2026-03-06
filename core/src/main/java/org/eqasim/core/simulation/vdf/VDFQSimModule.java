package org.eqasim.core.simulation.vdf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeSpeedCalculator;
import org.eqasim.core.simulation.vdf.travel_time.VDFLinkSpeedCalculator;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class VDFQSimModule extends AbstractQSimModule {
	private static final Logger logger = LogManager.getLogger(VDFQSimModule.class);

	@Override
	protected void configureQSim() {
		logger.info("Installing VDFQSimModule...");
		bind(EqasimLinkSpeedCalculator.class).to(VDFLinkSpeedCalculator.class);
	}

	@Provides
	@Singleton
	public VDFLinkSpeedCalculator provideVDFLinkSpeedCalculator(Population population, VDFTravelTime travelTime,
																CrossingPenalty crossingPenalty,
																BikeSpeedCalculator bikeSpeedCalculator) {
		return new VDFLinkSpeedCalculator(population, travelTime, crossingPenalty, bikeSpeedCalculator);
	}
}
