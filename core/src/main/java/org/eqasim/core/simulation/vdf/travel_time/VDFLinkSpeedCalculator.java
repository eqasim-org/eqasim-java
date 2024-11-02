package org.eqasim.core.simulation.vdf.travel_time;

import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;

public class VDFLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	private final VDFTravelTime travelTime;
	private final Population population;

	public VDFLinkSpeedCalculator(Population population, VDFTravelTime travelTime) {
		this.travelTime = travelTime;
		this.population = population;
	}

	@Override
	public double getMaximumVelocity(QVehicle vehicle, Link link, double time) {
		Person person = population.getPersons().get(vehicle.getDriver().getId());
		double calculatedTravelTime = travelTime.getLinkTravelTime(link, time, person, vehicle.getVehicle());
		return Math.min(link.getLength() / calculatedTravelTime, vehicle.getMaximumVelocity());
	}
}
