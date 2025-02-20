package org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment;


import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.matsim.core.router.NetworkRoutingProvider;
import org.matsim.core.router.RoutingModule;
import jakarta.inject.Provider;

public class ParkingAwareNetworkRoutingModuleProvider implements Provider<RoutingModule> {

    private final String mode;
    private final NetworkRoutingProvider networkRoutingProvider;
    private final Provider<NetworkWideParkingSpaceStore> networkWideParkingSpaceStoreProvider;
    private final Provider<ParkingSpaceAssignmentLogic> parkingSpaceAssignmentLogicProvider;

    public ParkingAwareNetworkRoutingModuleProvider(String mode, NetworkRoutingProvider networkRoutingProvider, Provider<NetworkWideParkingSpaceStore> networkWideParkingSpaceStoreProvider, Provider<ParkingSpaceAssignmentLogic> parkingSpaceAssignmentLogicProvider) {
        this.mode = mode;
        this.networkRoutingProvider = networkRoutingProvider;
        this.networkWideParkingSpaceStoreProvider = networkWideParkingSpaceStoreProvider;
        this.parkingSpaceAssignmentLogicProvider = parkingSpaceAssignmentLogicProvider;
    }

    @Override
    public RoutingModule get() {
        return new ParkingAwareNetworkRoutingModule(mode, networkRoutingProvider.get(), networkWideParkingSpaceStoreProvider.get(), parkingSpaceAssignmentLogicProvider.get());
    }
}
