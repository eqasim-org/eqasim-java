package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;
import org.matsim.vehicles.Vehicle;

public interface EqasimLinkSpeedCalculator extends LinkSpeedCalculator {
    /**
     * Gets the crossing penalty for a vehicle on a link at a given time.
     * Default implementation returns 0.0 (no penalty).
     * @param link The link.
     * @param time The time.
     * @param vehicleId The vehicle ID.
     * @return The crossing penalty in seconds.
     */
    default double getCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
        return 0.0;
    }

    default boolean isBike(QVehicle vehicle) {
        if (vehicle == null || vehicle.getVehicle() == null || vehicle.getVehicle().getType() == null) {
            return false; // treat as non-bike if any information is missing
        }
        return vehicle.getVehicle().getType().getNetworkMode().equals(TransportMode.bike);
    }

}
