package org.eqasim.core.components.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.vehicles.Vehicle;

/**
 * Immutable, allocation-free PCU lookup for the event-processing hot path.
 * Vehicle classification is performed once when the controller is initialized.
 */
public class VehiclePcuLookup {
    private final float[] pcuByVehicleIndex;

    public VehiclePcuLookup(Scenario scenario) {
        int maximumVehicleIndex = -1;

        for (Id<Vehicle> vehicleId : scenario.getVehicles().getVehicles().keySet()) {
            maximumVehicleIndex = Math.max(maximumVehicleIndex, vehicleId.index());
        }
        for (Id<Vehicle> vehicleId : scenario.getTransitVehicles().getVehicles().keySet()) {
            maximumVehicleIndex = Math.max(maximumVehicleIndex, vehicleId.index());
        }

        pcuByVehicleIndex = new float[maximumVehicleIndex + 1];

        for (Vehicle vehicle : scenario.getVehicles().getVehicles().values()) {
            boolean tobeIgnored = FlowUtils.isBike(vehicle.getId()) || FlowUtils.isCarPassenger(vehicle.getId());
            pcuByVehicleIndex[vehicle.getId().index()] = tobeIgnored
                    ? 0.0F
                    : (float) vehicle.getType().getPcuEquivalents();
        }

        // Classify once from the schedule, independent of vehicle ID naming.
        var busVehicleIds = FlowUtils.getBusVehicleIds(scenario);
        for (Vehicle vehicle : scenario.getTransitVehicles().getVehicles().values()) {
            if (busVehicleIds.contains(vehicle.getId())) {
                pcuByVehicleIndex[vehicle.getId().index()] = (float) vehicle.getType().getPcuEquivalents();
            }
        }
    }

    public float getPcu(Id<Vehicle> vehicleId) {
        int index = vehicleId.index();
        return index < pcuByVehicleIndex.length ? pcuByVehicleIndex[index] : 0.0F;
    }
}
