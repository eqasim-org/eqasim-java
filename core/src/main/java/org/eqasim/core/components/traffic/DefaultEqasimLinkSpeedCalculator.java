package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class DefaultEqasimLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	final private double crossingPenalty;

	public DefaultEqasimLinkSpeedCalculator(double crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}

		double maximumVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));

		if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return maximumVelocity;
		} else {
			double travelTime = link.getLength() / maximumVelocity;
			travelTime += crossingPenalty;
			return link.getLength() / travelTime;
		}
	}
}
