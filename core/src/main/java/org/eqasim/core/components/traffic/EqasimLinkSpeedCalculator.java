package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class EqasimLinkSpeedCalculator implements LinkSpeedCalculator {
	private final CrossingPenalty crossingPenalty;

	public EqasimLinkSpeedCalculator(CrossingPenalty crossingPenalty) {
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		double maximumVelocity = Math.min(vehicle.getMaximumVelocity(), link.getFreespeed(time));
		double travelTime = link.getLength() / maximumVelocity;
		travelTime += crossingPenalty.calculateCrossingPenalty(link);
		return link.getLength() / travelTime;
	}
}
