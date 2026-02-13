package org.eqasim.core.components.traffic;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic.bike.BikeLinkSpeedCalculator;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

public class EqasimTrafficQSimModule extends AbstractQSimModule {
	private static final Logger logger = LogManager.getLogger(EqasimTrafficQSimModule.class);

	@Override
	protected void configureQSim() {
		// here we check whether the bike is routed in the network or not, if it is, then we use the mixed vehicle speed calculator, otherwise we use the default one
		boolean bikeIsRouted = getConfig().routing().getNetworkModes().contains(TransportMode.bike);
		if (bikeIsRouted) {
			logger.info("Bike mode detected in routing configuration. Using BikeGradientBasedLinkSpeedCalculator for link speed calculations.");
			addLinkSpeedCalculatorBinding().to(BikeLinkSpeedCalculator.class);
			// bind(BikeLinkSpeedCalculator.class).to(BikeGradientBasedLinkSpeedCalculator.class);
		} else {
			logger.info("Bike mode not detected in routing configuration. Using DefaultEqasimLinkSpeedCalculator for link speed calculations.");
			addLinkSpeedCalculatorBinding().to(EqasimLinkSpeedCalculator.class);
			// bind(EqasimLinkSpeedCalculator.class).to(DefaultEqasimLinkSpeedCalculator.class);
		}
	}
}
