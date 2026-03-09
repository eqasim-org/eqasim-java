package org.eqasim.core.components.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.vehicles.Vehicle;

public class FlowUtils {

    public static double getCarPcu(Scenario scenario, Id<Vehicle> vehicleId){
        if (isBike(vehicleId)){
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
        if (isBus(event.getVehicleId())) {
            pcu = getBusPcu(scenario, event.getVehicleId());
        } else if (event.getVehicleId().toString().contains(":")) {
            pcu = getCarPcu(scenario, Id.createVehicleId(event.getVehicleId().toString()));
        }

        return pcu;
    }

    public static double getVehiclePcu(Scenario scenario, LinkLeaveEvent event){
        // this can be a transit vehicle or a normal vehicle
        double pcu = 0.0;
        if (isBus(event.getVehicleId())) {
            pcu = getBusPcu(scenario, event.getVehicleId());
        } else if (event.getVehicleId().toString().contains(":")) {
            pcu = getCarPcu(scenario, Id.createVehicleId(event.getVehicleId().toString()));
        }

        return pcu;
    }

    public static boolean isBike(Id<Vehicle> vehicleId){
        return vehicleId.toString().contains("bike");
    }

    public static boolean isBus(Id<Vehicle> vehicleId){
        return vehicleId.toString().contains("bus");
    }

    public static double getCountValue(double pcu, double sampleSize) {
        double epsilon = 1e-6;
        if (pcu < epsilon) {
            return 0.0; // do not count bikes
        } else if (pcu < 1.0-epsilon) {
            return sampleSize; // count buses as 1 count, but consider sample size for buses (e.g., if sample size is 0.1, then each bus counts as 10)
        } else {
            return 1.0; // count cars and trucks and LCV as 1 count

        }
    }

}
