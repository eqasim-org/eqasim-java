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
        return veh == null ? 0.0 : veh.getType().getPcuEquivalents();
    }

    public static double getBusPcu(Scenario scenario, Id<Vehicle> vehicleId){
        Vehicle veh = scenario.getTransitVehicles().getVehicles().get(vehicleId);
        return veh == null ? 0.0 : veh.getType().getPcuEquivalents();
    }

    public static double getVehiclePcu(Scenario scenario, LinkEnterEvent event){
        return getVehiclePcu(scenario, event.getVehicleId());
    }

    public static double getVehiclePcu(Scenario scenario, LinkLeaveEvent event){
        return getVehiclePcu(scenario, event.getVehicleId());
    }

    private static double getVehiclePcu(Scenario scenario, Id<Vehicle> vehicleId) {
        Vehicle vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
        if (vehicle != null) {
            return isBike(vehicleId) ? 0.0 : vehicle.getType().getPcuEquivalents();
        }

        if (isBus(vehicleId)) {
            Vehicle transitVehicle = scenario.getTransitVehicles().getVehicles().get(vehicleId);
            return transitVehicle == null ? 0.0 : transitVehicle.getType().getPcuEquivalents();
        }

        return 0.0;
    }

    public static boolean isBike(Id<Vehicle> vehicleId){
        return vehicleId.toString().contains("bike");
    }

    public static boolean isCarPassenger(Id<Vehicle> vehicleId){
        return vehicleId.toString().contains("car_passenger");
    }

    public static boolean isBus(Id<Vehicle> vehicleId){
        return vehicleId.toString().contains("bus");
    }

    public static float getCountValue(double pcu, double sampleSize) {
        double epsilon = 1e-3;
        if (pcu < epsilon) {
            return 0.0F; // do not count bikes
        } else if (pcu < 1.0-epsilon) {
            return (float) sampleSize; // count buses as 1 count, but consider sample size for buses (e.g., if sample size is 0.1, then each bus counts as 10)
        } else {
            return 1.0F; // count cars and trucks and LCV as 1 count

        }
    }

}
