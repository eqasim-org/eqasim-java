package org.eqasim.core.components.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;
import java.util.List;

/**
 * Immutable, allocation-free PCU lookup for the event-processing hot path.
 * Vehicle classification is performed once when the controller is initialized.
 */
public class VehiclePcuLookup {
    private final float[] pcuByVehicleIndex;

    public VehiclePcuLookup(Scenario scenario, FlowUtils flowUtils) {
        int maximumVehicleIndex = -1;

        for (Id<Vehicle> vehicleId : scenario.getVehicles().getVehicles().keySet()) {
            maximumVehicleIndex = Math.max(maximumVehicleIndex, vehicleId.index());
        }
        for (Id<Vehicle> vehicleId : scenario.getTransitVehicles().getVehicles().keySet()) {
            maximumVehicleIndex = Math.max(maximumVehicleIndex, vehicleId.index());
        }

        validateVehicleIndices(scenario, maximumVehicleIndex + 1);
        pcuByVehicleIndex = new float[maximumVehicleIndex + 1];

        for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
            pcuByVehicleIndex[vehicle.getId().index()] = (float) flowUtils.getVehiclePcu(vehicle.getId());
        }
        for (Vehicle vehicle : scenario.getTransitVehicles().getVehicles().values()) {
            pcuByVehicleIndex[vehicle.getId().index()] = (float) flowUtils.getVehiclePcu(vehicle.getId());
        }
    }

    /** Check once that regular and transit vehicles share a collision-free index space. */
    private static void validateVehicleIndices(Scenario scenario, int size) {
        Id<?>[] idsByIndex = new Id<?>[size];
        for (var vehicles : List.of(scenario.getVehicles(), scenario.getTransitVehicles())) {
            for (Id<Vehicle> id : vehicles.getVehicles().keySet()) {
                int index = id.index();
                if (index < 0) {
                    throw new IllegalStateException("Negative vehicle ID index for " + id + ": " + index);
                }
                Id<?> previous = idsByIndex[index];
                if (previous != null && !previous.equals(id)) {
                    throw new IllegalStateException("Vehicle IDs " + previous + " and " + id
                            + " share index " + index + "; PCU lookup requires unique indices across regular and transit vehicles");
                }
                idsByIndex[index] = id;
            }
        }
    }

    public float getPcu(Id<Vehicle> vehicleId) {
        int index = vehicleId.index();
        return index < pcuByVehicleIndex.length ? pcuByVehicleIndex[index] : 0.0F;
    }
}
