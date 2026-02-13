package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * Default implementation of EqasimLinkSpeedCalculator that calculates maximum velocity with crossing penalties.
 */
public class DefaultEqasimLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	final private CrossingPenalty crossingPenalty;

	/**
	 * Constructor for DefaultEqasimLinkSpeedCalculator.
	 * @param crossingPenalty The penalty calculator for crossings.
	 */
	public DefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	/**
	 * Calculates the crossing penalty for a vehicle on a link at a given time.
	 * @param link The link.
	 * @param time The time.
	 * @param vehicleId The vehicle ID.
	 * @return The crossing penalty in seconds.
	 */
	@Override
	public double getCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
		return crossingPenalty.calculateCrossingPenalty(link, time, vehicleId);
	}

	/**
	 * Gets the maximum velocity for a vehicle on a link, adjusted for crossing penalties.
	 * @param vehicle The vehicle.
	 * @param link The link.
	 * @param time The time.
	 * @return The maximum velocity.
	 */
	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double maximumVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double travelTime = link.getLength() / maximumVelocity;

		Id<Vehicle> vehicleId = vehicle.getVehicle().getId();
		travelTime += getCrossingPenalty(link, time, vehicleId);
		return link.getLength() / travelTime;
	}
}
