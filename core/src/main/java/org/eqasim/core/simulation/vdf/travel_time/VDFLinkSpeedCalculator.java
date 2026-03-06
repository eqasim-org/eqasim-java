package org.eqasim.core.simulation.vdf.travel_time;

import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.eqasim.core.components.traffic.bike.BikeSpeedCalculator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicle;
import org.matsim.vehicles.Vehicle;

public class VDFLinkSpeedCalculator implements EqasimLinkSpeedCalculator {
	private final VDFTravelTime travelTime;
	private final Population population;
	final private CrossingPenalty crossingPenalty;
	final private BikeSpeedCalculator bikeSpeedCalculator;

	public VDFLinkSpeedCalculator(Population population, VDFTravelTime travelTime, CrossingPenalty crossingPenalty, BikeSpeedCalculator bikeSpeedCalculator) {
		this.travelTime = travelTime;
		this.population = population;
		this.crossingPenalty = crossingPenalty;
		this.bikeSpeedCalculator = bikeSpeedCalculator;
	}

	@Override
	public double getMaximumCarVelocity(QVehicle vehicle, Link link, double time) {
		Person person = population.getPersons().get(vehicle.getDriver().getId());
		double calculatedTravelTime = travelTime.getLinkTravelTime(link, time, person, vehicle.getVehicle());

		// We include the crossing penalty here, and remove it from the VDFTravelTime. This is done because
		// when traffic lights module is activated, it needs dynamic data, not static data at the end of the iteration
		Id<Vehicle> vehicleId = vehicle.getVehicle().getId();
		calculatedTravelTime += crossingPenalty.calculateCrossingPenalty(link, time, vehicleId);

		return Math.min(link.getLength() / calculatedTravelTime, vehicle.getMaximumVelocity());
	}

	@Override
	public double getMaximumBikeVelocity(QVehicle vehicle, Link link, double time) {
		return bikeSpeedCalculator.getMaximumBikeVelocity(vehicle.getVehicle(), link, time);
	}
}
