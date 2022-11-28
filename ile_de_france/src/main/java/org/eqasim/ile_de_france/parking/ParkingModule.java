package org.eqasim.ile_de_france.parking;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ParkingModule extends AbstractModule {
	private final double urbanParkingCost_EUR_h;
	private final File parkingPressureFile;

	public ParkingModule(File parkingPressureFile, double urbanParkingCost_EUR_h) {
		this.urbanParkingCost_EUR_h = urbanParkingCost_EUR_h;
		this.parkingPressureFile = parkingPressureFile;
	}

	@Override
	public void install() {

	}

	@Provides
	@Singleton
	ParkingInformation provideParkingInformation(ParkingPressureData parkingPressure) {
		return new ParkingInformation(parkingPressure, urbanParkingCost_EUR_h);
	}

	@Provides
	@Singleton
	ParkingPressureData provideParkingPressureData(Network network) throws IOException {
		return ParkingPressureData.load(network, parkingPressureFile);
	}
}
