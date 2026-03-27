package org.eqasim.core.tools.schedule;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;
import org.matsim.vehicles.VehiclesFactory;

public class ExtendSchedule {
	private final static double H24 = 24 * 3600.0;

	private final double endTime;

	public ExtendSchedule(double endTime) {
		this.endTime = endTime;
	}

	private Id<Departure> prefixDeparture(Id<Departure> baseId, int extension) {
		return Id.create(baseId.toString() + ":ext:" + extension, Departure.class);
	}

	private Id<Vehicle> prefixVehicle(Id<Vehicle> baseId, int extension) {
		return Id.create(baseId.toString() + ":ext:" + extension, Vehicle.class);
	}

	public void process(TransitSchedule schedule, Vehicles vehicles) {
		TransitScheduleFactory scheduleFactory = schedule.getFactory();
		VehiclesFactory vehiclesFactory = vehicles.getFactory();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Departure> addedDepartures = new LinkedList<>();

				for (Departure departure : transitRoute.getDepartures().values()) {
					double updatedDepartureTime = departure.getDepartureTime() + H24;
					int extensionIndex = 1;

					while (updatedDepartureTime <= endTime) {
						Departure updatedDeparture = scheduleFactory.createDeparture( //
								prefixDeparture(departure.getId(), extensionIndex), //
								updatedDepartureTime);
						addedDepartures.add(updatedDeparture);

						Vehicle vehicle = vehicles.getVehicles().get(departure.getVehicleId());

						Id<Vehicle> updatedVehicleId = prefixVehicle(vehicle.getId(), extensionIndex);
						updatedDeparture.setVehicleId(updatedVehicleId);

						if (!vehicles.getVehicles().containsKey(updatedVehicleId)) {
							Vehicle updatedVehicle = vehiclesFactory.createVehicle(updatedVehicleId, vehicle.getType());
							vehicles.addVehicle(updatedVehicle);
						}

						extensionIndex++;
						updatedDepartureTime += H24;
					}
				}

				for (Departure extension : addedDepartures) {
					transitRoute.addDeparture(extension);
				}
			}
		}
	}
}