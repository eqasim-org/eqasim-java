package org.eqasim.core.components.traffic;

import java.util.Map;

import org.eqasim.core.analysis.ComputeDelayTrafficLights;
import org.eqasim.core.analysis.PrepareInputDataIntersections;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.core.mobsim.qsim.qnetsimengine.linkspeedcalculator.LinkSpeedCalculator;

public class EqasimLinkSpeedCalculatorTrafficLightsWebster implements LinkSpeedCalculator {
	final private LinkSpeedCalculator delegate;
	final private double crossingPenalty;
	final private Map<Id<Link>, double[]> trafficLightsDelays;

	public EqasimLinkSpeedCalculatorTrafficLightsWebster(LinkSpeedCalculator delegate, double crossingPenalty) {
		this.delegate = delegate;
		this.crossingPenalty = crossingPenalty;
		PrepareInputDataIntersections p = new PrepareInputDataIntersections();
		ComputeDelayTrafficLights delay = new ComputeDelayTrafficLights(p);
		delay.writeCSV_webster("/home/asallard/Dokumente/Projects/Traffic lights - Zuerich/Simulation results/60it_webster/intersections_webster.csv");
		this.trafficLightsDelays = delay.compute_all_delays_webster().get(0);
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		boolean isMajor = true;

		for (Link other : link.getToNode().getInLinks().values()) {
			if (other.getCapacity() >= link.getCapacity()) {
				isMajor = false;
			}
		}
		
		if (trafficLightsDelays.containsKey(link.getId())) {
			double travelTime =  link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			int hour = ((int) time / 3600) % 30;
			travelTime += trafficLightsDelays.get(link.getId())[hour];
			return link.getLength() / travelTime;
		}
		else if (isMajor || link.getToNode().getInLinks().size() == 1) {
			return delegate.getMaximumVelocity(vehicle, link, time);
		} 
		else {
			double travelTime =  link.getLength() / delegate.getMaximumVelocity(vehicle, link, time);
			travelTime += crossingPenalty;
			return link.getLength() / travelTime;
		}
	}
}