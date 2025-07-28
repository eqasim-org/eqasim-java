package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

public class DefaultEqasimLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	final private CrossingPenalty crossingPenalty;

	public DefaultEqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double maximumVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double travelTime = link.getLength() / maximumVelocity;

		Id<Vehicle> vehicleId = vehicle.getVehicle().getId();
		travelTime += crossingPenalty.calculateCrossingPenalty(link, time, vehicleId);
		return link.getLength() / travelTime;
	}
}
