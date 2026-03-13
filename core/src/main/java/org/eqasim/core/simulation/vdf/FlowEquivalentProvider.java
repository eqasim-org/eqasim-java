package org.eqasim.core.simulation.vdf;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

public class FlowEquivalentProvider {
    private final VehicleType defaultVehicleType = VehicleUtils.createDefaultVehicleType();

    private final Vehicles vehicles;
    private final Vehicles transitVehicles;

    private final IdMap<Vehicle, Double> cache = new IdMap<>(Vehicle.class);

    public FlowEquivalentProvider(Vehicles vehicles, Vehicles transitVehicles) {
        this.vehicles = vehicles;
        this.transitVehicles = transitVehicles;
    }

    private VehicleType getVehicleType(Id<Vehicle> vehicleId) {
        Vehicle vehicle = vehicles.getVehicles().get(vehicleId);

        if (vehicle == null) {
            vehicle = transitVehicles.getVehicles().get(vehicleId);
        }

        return vehicle == null ? defaultVehicleType : vehicle.getType();
    }

    public double getFlowEquivalent(Id<Vehicle> vehicleId) {
        if (vehicleId == null) {
            return defaultVehicleType.getPcuEquivalents() / defaultVehicleType.getFlowEfficiencyFactor();
        }

        return cache.computeIfAbsent(vehicleId, id -> {
            VehicleType vehicleType = getVehicleType(vehicleId);
            return vehicleType.getPcuEquivalents() / vehicleType.getFlowEfficiencyFactor();
        });
    }
}
