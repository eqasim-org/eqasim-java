package org.eqasim.core.scenario.routing;

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Provider for Parking Availability Data
 * 
 * @author akramelb
 */
public class ParkingAvailabilityModule extends AbstractModule {

    @Provides
	@Singleton
	ParkingAvailabilityData provideParkingAvailabilityData(Network network) throws IOException {
		return ParkingAvailabilityData.loadFromAttributes(network);
	}

	@Override
	public void install() {
		// Nothing, idk what I'm supposed to do here
	}

}
