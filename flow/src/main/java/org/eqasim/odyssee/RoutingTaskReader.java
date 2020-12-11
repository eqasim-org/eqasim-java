package org.eqasim.odyssee;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class RoutingTaskReader {
	public List<RoutingTask> read(File path) throws IOException {
		List<RoutingTask> tasks = new LinkedList<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

		String line = null;
		List<String> header = null;

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));

			if (header == null) {
				header = row;
			} else {
				String personId = row.get(header.indexOf("person_id"));
				String officeId = row.get(header.indexOf("office_id"));

				double originX = Double.parseDouble(row.get(header.indexOf("origin_x")));
				double originY = Double.parseDouble(row.get(header.indexOf("origin_y")));
				double destinationX = Double.parseDouble(row.get(header.indexOf("destination_x")));
				double destinationY = Double.parseDouble(row.get(header.indexOf("destination_y")));

				tasks.add(new RoutingTask(personId, officeId, originX, originY, destinationX, destinationY));
			}
		}

		reader.close();
		return tasks;
	}
}
