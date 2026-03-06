package org.eqasim.core.components.traffic;

import org.eqasim.core.components.traffic.bike.BikeSpeedCalculator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

/**
 * Default implementation of EqasimLinkSpeedCalculator that calculates maximum velocity with crossing penalties.
 */
public class DefaultEqasimLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	final private CrossingPenalty crossingPenalty;
	final private BikeSpeedCalculator bikeSpeedCalculator;

	public DefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty, BikeSpeedCalculator bikeSpeedCalculator) {
		this.crossingPenalty = crossingPenalty;
		this.bikeSpeedCalculator = bikeSpeedCalculator;
	}

	@Override
	public double getCrossingPenalty(Link link, double time, Id<Vehicle> vehicleId) {
		return crossingPenalty.calculateCrossingPenalty(link, time, vehicleId);
	}

	@Override
	public double getMaximumCarVelocity(QVehicle vehicle, Link link, double time) {
		double maximumVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double travelTime = link.getLength() / maximumVelocity;

		Id<Vehicle> vehicleId = vehicle.getVehicle().getId();
		travelTime += getCrossingPenalty(link, time, vehicleId);
		return link.getLength() / travelTime;
	}

	@Override
	public double getMaximumBikeVelocity(QVehicle vehicle, Link link, double time) {
		return bikeSpeedCalculator.getMaximumBikeVelocity(vehicle.getVehicle(), link, time);
	}


}
