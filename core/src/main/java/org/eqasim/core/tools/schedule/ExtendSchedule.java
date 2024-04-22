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
	private final static String DEPATURE_PREFIX = "extension:";
	private final static double OFFSET = 24.0 * 3600.0;

	private final double time;

	public ExtendSchedule(double time) {
		this.time = time;
	}

	public void process(TransitSchedule schedule) {
		TransitScheduleFactory factory = schedule.getFactory();

		for (TransitLine transitLine : schedule.getTransitLines().values()) {
			for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
				List<Departure> extensions = new LinkedList<>();

				for (Departure departure : transitRoute.getDepartures().values()) {
					if (departure.getDepartureTime() <= time) {
						Departure extension = factory.createDeparture( //
								Id.create(DEPATURE_PREFIX + departure.getId().toString(), Departure.class), //
								departure.getDepartureTime() + OFFSET);

						extension.setVehicleId(departure.getVehicleId());
						extensions.add(extension);
					}
				}

				for (Departure extension : extensions) {
					transitRoute.addDeparture(extension);
				}
			}
		}
	}
}