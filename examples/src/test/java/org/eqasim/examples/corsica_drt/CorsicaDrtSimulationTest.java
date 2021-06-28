package org.eqasim.examples.corsica_drt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class CorsicaDrtSimulationTest {
	@Test
	public void testCorsicaDrtSimulationTest() throws ConfigurationException, IOException {
		RunCorsicaDrtSimulation.main(new String[] { "--mode-parameter:drt.alpha_u", "20.0" // To be sure that we see
																							// some DRT trips after 2
																							// iterations
		});

		int arrivalCount = 0;

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream("simulation_output/eqasim_drt_passenger_rides.csv")));

		String raw = null;
		List<String> header = null;

		while ((raw = reader.readLine()) != null) {
			List<String> line = Arrays.asList(raw.split(";"));

			if (header == null) {
				header = line;
			} else {
				double arrivalTime = Double.parseDouble(line.get(header.indexOf("arrival_time")));

				if (Double.isFinite(arrivalTime)) {
					arrivalCount++;
				}
			}
		}

		reader.close();
		FileUtils.deleteDirectory(new File("simulation_output"));

		Assert.assertTrue(arrivalCount > 0);
	}
}
