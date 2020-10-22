package org.eqasim.core.analysis.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class PublicTransportTripWriter {
	final private Collection<PublicTransportTripItem> trips;
	final private String delimiter;

	public PublicTransportTripWriter(Collection<PublicTransportTripItem> trips) {
		this(trips, ";");
	}

	public PublicTransportTripWriter(Collection<PublicTransportTripItem> trips, String delimiter) {
		this.trips = trips;
		this.delimiter = delimiter;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (PublicTransportTripItem trip : trips) {
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
				"access_stop_id", //
				"egress_stop_id", //
				"transit_line_id", //
				"transit_route_id" //
		});
	}

	private String formatTrip(PublicTransportTripItem trip) {
		return String.join(delimiter, new String[] { //
				trip.personId.toString(), //
				String.valueOf(trip.personTripId), //
				trip.accessStopId.toString(), //
				trip.egressStopId.toString(), //
				trip.transitLineId.toString(), //
				trip.transitRouteId.toString() //
		});
	}
}
