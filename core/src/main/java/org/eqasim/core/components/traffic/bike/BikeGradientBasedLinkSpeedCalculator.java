package org.eqasim.core.components.traffic.bike;

import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * Calculates speeds for bike vehicles with gradient-based adjustments and crossing penalties.
 * Throws an exception for non-bike vehicles.
 */
public class BikeGradientBasedLinkSpeedCalculator implements BikeLinkSpeedCalculator {
    private final EqasimLinkSpeedCalculator carSpeedCalculator;

    /**
     * Constructor for BikeGradientBasedLinkSpeedCalculator.
     * @param carSpeedCalculator The default speed calculator for non-bike vehicles.
     */
    public BikeGradientBasedLinkSpeedCalculator(EqasimLinkSpeedCalculator carSpeedCalculator) {
        this.carSpeedCalculator = carSpeedCalculator;
    }

    /**
     * Gets the maximum velocity for a vehicle on a link, handling bikes differently.
     * @param vehicle The vehicle.
     * @param link The link.
     * @param time The time.
     * @return The maximum velocity.
     */
    @Override
    public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
        if (isBike(vehicle)) {
            return maximumBikeSpeed(vehicle.getVehicle(), link, time);
        } else {
            return carSpeedCalculator.getMaximumVelocity(vehicle, link, time);
        }
    }

    /**
     * Calculates the maximum speed for a bike, considering gradient and crossing penalties.
     * @param vehicle The bike vehicle.
     * @param link The link.
     * @param time The time.
     * @return The maximum bike speed.
     */
    @Override
    public double maximumBikeSpeed(Vehicle vehicle, Link link, double time) {
        double maximumVelocity = link.getFreespeed(time);
        if (!(vehicle == null)){
            maximumVelocity = Math.min(vehicle.getType().getMaximumVelocity(), maximumVelocity);
        }

        double gradientBasedSpeed = getLinkSpeed(link);
        double expectedVelocity = Math.min(maximumVelocity, gradientBasedSpeed);

        // if the bike speed is already slow, we do not need to add the crossing penalty
        if (expectedVelocity <= 3.0) {
            return expectedVelocity;
        }

        // else we add a delay, however, only the half of it is added, because I assume that the delay is not as bad for bikes as for cars
        // this need to be further investigated, but for now, I assume that the crossing penalty is only half as bad for bikes as for cars, because they can more easily maneuver around obstacles and other vehicles
        // TODO: add a proper delay for bikes
        double travelTime = link.getLength() / expectedVelocity;
        travelTime += (carSpeedCalculator.getCrossingPenalty(link, time, vehicle.getId()) / 2.0);
        double adjustedSpeed = link.getLength() / travelTime;
        return Math.max(1.0, Math.min(expectedVelocity, adjustedSpeed)); // Ensure speed is no less than 1 m/s and does not exceed expected velocity

    }

    /**
     * Calculates the link speed based on gradient for bikes.
     * @param link The link.
     * @return The gradient-based speed in m/s.
     */
    private double getLinkSpeed(Link link) {
        // Get gradient in percentage
        double gradient = percentageGradient(link);

        // Calculate speed using your formula (result in m/s)
        double speed_mps = 5.90
                - 0.3191 * gradient
                - 0.010237 * Math.pow(gradient, 2);

        // Clamp to reasonable values (avoid too low and too high speeds)
        return Math.max(2.0, Math.min(7.5, speed_mps));
    }

    /**
     * Calculates the percentage gradient of a link.
     * @param link The link.
     * @return The gradient in percentage.
     */
    public static double percentageGradient(Link link){
        Coord fromCoord = link.getFromNode().getCoord();
        Coord toCoord = link.getToNode().getCoord();
        if (!fromCoord.hasZ() || !toCoord.hasZ()) {
            // If Z-coordinates are not available, assume flat terrain
            return 0.0;
        } else {
            double horizontalDistance = link.getLength(); // in meters
            if (horizontalDistance < 1.0) {
                return 0.0; // treat as flat
            }
            double fromZ = fromCoord.getZ();
            double toZ = toCoord.getZ();
            if (fromZ == 0.0 || toZ == 0.0) {
                return 0.0; // these are the nodes where we do not have observations, so we assume flat terrain for the now
            }
            double verticalChange = toZ - fromZ; // in meters
            return (verticalChange / horizontalDistance) * 100.0; // convert to percentage
        }
    }

    private boolean isBike(QVehicle vehicle) {
        if (vehicle == null || vehicle.getVehicle() == null || vehicle.getVehicle().getType() == null) {
            return false; // treat as non-bike if any information is missing
        }
        return vehicle.getVehicle().getType().getNetworkMode().equals(TransportMode.bike);
    }

}
