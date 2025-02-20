package org.eqasim.core.simulation.modes.parking_aware_car;

import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.DestinationActivityTypeDependentParkingAssignment;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingAwareNetworkRoutingModuleProvider;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.NetworkRoutingProvider;

public class ParkingAwareNetworkModeModule extends AbstractModule {

    public ParkingAwareNetworkModeModule() {

    }

    @Override
    public void install() {
        bind(ParkingSpaceAssignmentLogic.class).to(DestinationActivityTypeDependentParkingAssignment.class);
        bind(NetworkWideParkingSpaceStore.class);
        addRoutingModuleBinding("car").toProvider(new ParkingAwareNetworkRoutingModuleProvider("car",
                new NetworkRoutingProvider("car"),
                getProvider(TypeLiteral.get(NetworkWideParkingSpaceStore.class)),
                getProvider(TypeLiteral.get(ParkingSpaceAssignmentLogic.class))));
    }

    @Provides
    public NetworkWideParkingSpaceStore provideNetworkWideParkingSpaceStore(Network network) {
        return new NetworkWideParkingSpaceStore(network);
    }
}
