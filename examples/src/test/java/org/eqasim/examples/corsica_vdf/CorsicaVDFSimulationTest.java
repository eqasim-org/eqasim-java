package org.eqasim.examples.corsica_vdf;

import java.io.IOException;

import org.junit.Test;
import org.matsim.core.config.CommandLine.ConfigurationException;

public class CorsicaVDFSimulationTest {
	@Test
	public void testCorsicaVDFSimulationTest() throws ConfigurationException, IOException {
		RunCorsicaVDFSimulation.main(new String[] {});
	}

	@Test
	public void testCorsicaVDFEngineSimulationTest() throws ConfigurationException, IOException {
		RunCorsicaVDFEngineSimulation.main(new String[] {});
	}
}
