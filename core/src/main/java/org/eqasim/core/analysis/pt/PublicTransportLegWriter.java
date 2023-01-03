package org.eqasim.core.analysis.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class PublicTransportLegWriter {
	final private Collection<PublicTransportLegItem> trips;
	final private String delimiter;

	public PublicTransportLegWriter(Collection<PublicTransportLegItem> trips) {
		this(trips, ";");
	}

	public PublicTransportLegWriter(Collection<PublicTransportLegItem> trips, String delimiter) {
		this.trips = trips;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (PublicTransportLegItem trip : trips) {
			writer.write(formatTrip(trip) + "\n");
			writer.flush();
		}

		writer.flush();
		writer.close();
	}

	private String formatHeader() {
		return String.join(delimiter, new String[] { //
				"person_id", //
				"person_trip_id", //
				"leg_index", //
				"access_stop_id", //
				"egress_stop_id", //
				"transit_line_id", //
				"transit_route_id", //
				"access_area_id", //
				"egress_area_id", //
				"transit_mode" //
		});
	}

	private String formatTrip(PublicTransportLegItem trip) {
		return String.join(delimiter, new String[] { //
				trip.personId.toString(), //
				String.valueOf(trip.personTripId), //
				String.valueOf(trip.legIndex), //
				trip.accessStopId.toString(), //
				trip.egressStopId.toString(), //
				trip.transitLineId.toString(), //
				trip.transitRouteId.toString(), //
				trip.accessAreaId == null ? "" : trip.accessAreaId.toString(), //
				trip.egressAreaId == null ? "" : trip.egressAreaId.toString(), //
				trip.transitMode //
		});
	}
}
