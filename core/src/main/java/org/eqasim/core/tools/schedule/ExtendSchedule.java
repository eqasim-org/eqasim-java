package org.eqasim.core.tools.schedule;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;

public class ExtendSchedule {
	private final static double H24 = 24 * 3600.0;

	private final double endTime;

	public ExtendSchedule(double endTime) {
		this.endTime = endTime;
	}

	private Id<Departure> prefix(Id<Departure> baseId, int extension) {
		return Id.create(baseId.toString() + ":ext:" + extension, Departure.class);
	}

	public void process(TransitSchedule schedule) {
		TransitScheduleFactory factory = schedule.getFactory();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Departure> extensions = new LinkedList<>();

				for (Departure departure : transitRoute.getDepartures().values()) {
					double updatedDepartureTime = departure.getDepartureTime() + H24;
					int extensionIndex = 1;

					while (updatedDepartureTime <= endTime) {
						Departure extension = factory.createDeparture( //
								prefix(departure.getId(), extensionIndex), //
								updatedDepartureTime);

						extension.setVehicleId(departure.getVehicleId());
						extensions.add(extension);

						extensionIndex++;
						updatedDepartureTime += H24;
					}
				}

				for (Departure extension : extensions) {
					transitRoute.addDeparture(extension);
				}
			}
		}
	}
}