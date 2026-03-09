package org.eqasim.core.components.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.vehicles.Vehicle;

public class FlowUtils {

    public static double getCarPcu(Scenario scenario, Id<Vehicle> vehicleId){
        if (vehicleId.toString().contains("bike")){
            return 0.0; // bikes are not counted in the flow
        }
        Vehicle veh = scenario.getVehicles().getVehicles().get(vehicleId);
        return scenario.getVehicles().getVehicleTypes().get(veh.getType().getId()).getPcuEquivalents();
    }

    public static double getBusPcu(Scenario scenario, Id<Vehicle> vehicleId){
        Vehicle veh = scenario.getTransitVehicles().getVehicles().get(vehicleId);
        return scenario.getTransitVehicles().getVehicleTypes().get(veh.getType().getId()).getPcuEquivalents();
    }

    public static double getVehiclePcu(Scenario scenario, LinkEnterEvent event){
        // this can be a transit vehicle or a normal vehicle
        double pcu = 0.0;
        if (event.getVehicleId().toString().contains("bus")) {
            pcu = getBusPcu(scenario, event.getVehicleId());
        } else if (event.getVehicleId().toString().contains(":")) {
            pcu = getCarPcu(scenario, Id.createVehicleId(event.getVehicleId().toString()));
        }

        return pcu;
    }
}
