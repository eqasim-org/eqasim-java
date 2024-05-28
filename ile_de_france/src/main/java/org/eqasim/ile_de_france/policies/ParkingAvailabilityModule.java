package org.eqasim.ile_de_france.policies;

import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ParkingAvailabilityModule extends AbstractModule {

    @Provides
	@Singleton
	ParkingAvailabilityData provideParkingAvailabilityData(Network network) throws IOException {
		return ParkingAvailabilityData.loadFromAttributes(network);
	}

	@Provides
	@Singleton
	ParkingAssignment provideParkingAssignment(Network network, ParkingAvailabilityData parkingAvailabilityData) {
		return new ParkingAssignment(network, parkingAvailabilityData);
	}

	@Override
	public void install() {
		// Nothing, idk what I'm supposed to do here
	}

}
