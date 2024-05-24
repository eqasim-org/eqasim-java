package org.eqasim.ile_de_france.parking;

import java.io.IOException;

import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFParkingPredictor;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ParkingModule extends AbstractModule {
	private final double urbanParkingCost_EUR_h;

	public ParkingModule(double urbanParkingCost_EUR_h) {
		this.urbanParkingCost_EUR_h = urbanParkingCost_EUR_h;
	}

	@Override
	public void install() {
		bind(IDFParkingPredictor.class).asEagerSingleton();
	}

	@Provides
	@Singleton
	ParkingInformation provideParkingInformation(ParkingPressureData parkingPressure) {
		return new ParkingInformation(parkingPressure, urbanParkingCost_EUR_h);
	}

	@Provides
	@Singleton
	ParkingPressureData provideParkingPressureData(Network network) throws IOException {
		return ParkingPressureData.loadFromAttributes(network);
	}
}
