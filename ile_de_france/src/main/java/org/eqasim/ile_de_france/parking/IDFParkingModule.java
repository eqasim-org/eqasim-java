package org.eqasim.ile_de_france.parking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class IDFParkingModule extends AbstractModule {
    @Override
    public void install() {
    }

    @Provides
    @Singleton
    ParkingPressure provideParkingPressure(Network network) {
        return new ParkingPressure(network);
    }

    @Provides
    @Singleton
    ParkingTariff provideParkingTariff(Network network) {
        return new ParkingTariff(network);
    }
}
