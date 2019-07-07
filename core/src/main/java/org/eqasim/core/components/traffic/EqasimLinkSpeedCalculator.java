package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class EqasimLinkSpeedCalculator implements LinkSpeedCalculator {
	final private LinkSpeedCalculator delegate;
	final private double crossingPenalty;

	public EqasimLinkSpeedCalculator(LinkSpeedCalculator delegate, double crossingPenalty) {
		this.delegate = delegate;
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

		if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return delegate.getMaximumVelocity(vehicle, link, time);
		} else {
			double travelTime =  link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			travelTime += crossingPenalty;
			return link.getLength() / travelTime;
		}
	}
}
