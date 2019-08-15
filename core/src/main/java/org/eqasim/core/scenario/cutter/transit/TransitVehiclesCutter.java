package org.eqasim.core.scenario.cutter.transit;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.Vehicles;

public class TransitVehiclesCutter {
	private final static Logger log = Logger.getLogger(TransitVehiclesCutter.class);

	private final TransitSchedule schedule;

	public TransitVehiclesCutter(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	public void run(Vehicles vehicles) {
		if (vehicles.getVehicles().size() == 0) {
			log.warn("Not cutting vehicles as they are not defined.");
			return;
		}

		log.info("Cutting transit vehicles ...");

		Set<Vehicle> unusedVehicles = new HashSet<>(vehicles.getVehicles().values());
		int originalNumberOfVehicles = vehicles.getVehicles().size();

		for (TransitLine line : schedule.getTransitLines().values()) {
			for (TransitRoute route : line.getRoutes().values()) {
				for (Departure departure : route.getDepartures().values()) {
					Vehicle vehicle = vehicles.getVehicles().get(departure.getVehicleId());

					if (vehicle == null) {
						throw new IllegalArgumentException("Cannot find vehicle " + departure.getVehicleId());
					}

					unusedVehicles.remove(vehicle);
				}
			}
		}

		unusedVehicles.forEach(v -> vehicles.removeVehicle(v.getId()));
		int finalNumberOfVehicles = vehicles.getVehicles().size();

		log.info("Finished cutting transit vehicles.");
		log.info("  Number of vehicles before:" + originalNumberOfVehicles);
		log.info("  Number of vehicles after:" + finalNumberOfVehicles);
	}
}
