package org.eqasim.odyssee;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

public class RoutingResultWriter {
	private final List<RoutingResult> results;

	public RoutingResultWriter(List<RoutingResult> results) {
		this.results = results;
	}

	public List<RoutingTask> write(File path) throws IOException {
		List<RoutingTask> tasks = new LinkedList<>();

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

		writer.write(String.join(";", new String[] { //
				"person_id", //
				"office_id", //
				"car_travel_time", //
				"car_distance", //
				"pt_travel_time", //
				"pt_distance" //
		}) + "\n");

		for (RoutingResult result : results) {
			writer.write(String.join(";", new String[] { //
					result.personId, //
					result.officeId, //
					String.valueOf(result.carTravelTime), //
					String.valueOf(result.carDistance), //
					String.valueOf(result.ptTravelTime), //
					String.valueOf(result.ptDistance), //
			}) + "\n");
		}

		writer.close();
		return tasks;
	}
}
