package org.eqasim.core.components.flow;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.vehicles.Vehicle;

public class FlowUtils {
    private final Scenario scenario;
    private final IdSet<Vehicle> busIds = new IdSet<>(Vehicle.class);
    private final IdSet<Vehicle> bikeIds = new IdSet<>(Vehicle.class);
    private final IdSet<Vehicle> carPassengerIds = new IdSet<>(Vehicle.class);

    public FlowUtils(Scenario scenario) {
        this.scenario = scenario;
        scenario.getVehicles().getVehicles().values().forEach(this::classifyVehicle);
        scenario.getTransitVehicles().getVehicles().values().forEach(this::classifyVehicle);
        // Transit vehicles often use network mode car or pt, so use the route mode too.
        scenario.getTransitSchedule().getTransitLines().values().forEach(line -> line.getRoutes().values().forEach(route -> {
            if ("bus".equals(route.getTransportMode())) {
                route.getDepartures().values().forEach(departure -> {
                    if (departure.getVehicleId() != null) {
                        busIds.add(departure.getVehicleId());
                    }
                });
            }
        }));
    }

    private void classifyVehicle(Vehicle vehicle) {
        String mode = vehicle.getType().getNetworkMode();
        if (TransportMode.bike.equals(mode)) {
            bikeIds.add(vehicle.getId());
        } else if ("bus".equals(mode)) {
            busIds.add(vehicle.getId());
        } else if ("car_passenger".equals(mode)) {
            carPassengerIds.add(vehicle.getId());
        }
    }

    public boolean isBike(Id<Vehicle> vehicleId) {
        return vehicleId != null && bikeIds.contains(vehicleId);
    }

    public boolean isBus(Id<Vehicle> vehicleId) {
        return vehicleId != null && busIds.contains(vehicleId);
    }

    public boolean isCarPassenger(Id<Vehicle> vehicleId) {
        return vehicleId != null && carPassengerIds.contains(vehicleId);
    }

    public double getVehiclePcu(Id<Vehicle> vehicleId) {
        if (vehicleId == null || isBike(vehicleId) || isCarPassenger(vehicleId)) {
            return 0.0;
        }
        Vehicle vehicle = isBus(vehicleId) ? scenario.getTransitVehicles().getVehicles().get(vehicleId) : null;
        if (vehicle == null) {
            vehicle = scenario.getVehicles().getVehicles().get(vehicleId);
        }
        return vehicle == null ? 0.0 : vehicle.getType().getPcuEquivalents();
    }

    /** Daily-count weight: bikes contribute zero, buses the population sample size, other vehicles one.
     * Bus services represent the full timetable; their weight keeps daily counts on the sampled scale.
     * This is separate from PCU weighting, which is used to compute traffic flow.
     */
    public float getCountValue(Id<Vehicle> vehicleId, double sampleSize) {
        if (isBike(vehicleId)) {
            return 0.0F;
        }
        return isBus(vehicleId) ? (float) sampleSize : 1.0F;
    }

    /** Road approaches used by both intersection models; pt alone may also mean rail. */
    public static boolean isCarOrBusLink(Link link) {
        return link.getAllowedModes() != null &&
                (link.getAllowedModes().contains(TransportMode.car) || link.getAllowedModes().contains("bus"));
    }
}
