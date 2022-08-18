package org.eqasim.core.analysis.legs;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

import org.eqasim.core.analysis.DistanceUnit;

public class LegWriter {
	final private Collection<LegItem> legs;
	final private String delimiter;

	final private DistanceUnit inputUnit;
	final private DistanceUnit outputUnit;

	public LegWriter(Collection<LegItem> legs, DistanceUnit inputUnit, DistanceUnit outputUnit) {
		this(legs, inputUnit, outputUnit, ";");
	}

	public LegWriter(Collection<LegItem> legs, DistanceUnit inputUnit, DistanceUnit outputUnit, String delimiter) {
		this.legs = legs;
		this.delimiter = delimiter;
		this.inputUnit = inputUnit;
		this.outputUnit = outputUnit;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		writer.write(formatHeader() + "\n");
		writer.flush();

		for (LegItem leg : legs) {
			writer.write(formatLeg(leg) + "\n");
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
				"origin_x", //
				"origin_y", //
				"destination_x", //
				"destination_y", "departure_time", //
				"travel_time", //
				"vehicle_distance", //
				"routed_distance", //
				"mode", //
				"euclidean_distance" //
		});
	}

	/**
	 * Gets a factor to convert any other unit to meters
	 */
	private double getUnitFactor(DistanceUnit unit) {
		double factor = Double.NaN;

		switch (inputUnit) {
		case foot:
			factor = 0.3048;
			break;
		case kilometer:
			factor = 1e3;
			break;
		case meter:
			factor = 1.0;
			break;
		case mile:
			factor = 1609.344;
			break;
		default:
			throw new IllegalStateException("Unknown input unit");
		}

		return factor;
	}

	private String formatLeg(LegItem leg) {
		double inputFactor = getUnitFactor(inputUnit);
		double outputFactor = 1.0 / getUnitFactor(outputUnit);

		return String.join(delimiter, new String[] { //
				leg.personId.toString(), //
				String.valueOf(leg.personTripId), //
				String.valueOf(leg.legIndex), //
				String.valueOf(leg.origin.getX()), //
				String.valueOf(leg.origin.getY()), //
				String.valueOf(leg.destination.getX()), //
				String.valueOf(leg.destination.getY()), //
				String.valueOf(leg.departureTime), //
				String.valueOf(leg.travelTime), //
				String.valueOf(leg.vehicleDistance * inputFactor * outputFactor), //
				String.valueOf(leg.routedDistance * inputFactor * outputFactor), //
				String.valueOf(leg.mode), //
				String.valueOf(leg.euclideanDistance * inputFactor * outputFactor) //
		});
	}
}
