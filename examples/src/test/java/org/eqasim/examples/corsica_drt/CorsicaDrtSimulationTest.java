package org.eqasim.examples.corsica_drt;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class CorsicaDrtSimulationTest {
	@Test
	public void testCorsicaDrtSimulationTest() throws ConfigurationException, IOException {
		RunCorsicaDrtSimulation.main(new String[] {});
		FileUtils.deleteDirectory(new File("simulation_output"));
	}
}
