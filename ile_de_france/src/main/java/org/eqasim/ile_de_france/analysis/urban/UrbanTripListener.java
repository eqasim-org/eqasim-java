package org.eqasim.ile_de_france.analysis.urban;

import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;

public class UrbanTripListener implements StartupListener {
	private final Population population;
	private final OutputDirectoryHierarchy outputHierarchy;

	public UrbanTripListener(Population population, OutputDirectoryHierarchy outputHierarchy) {
		this.population = population;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		try {
			String outputPath = outputHierarchy.getOutputFilename("urban.csv");
			new UrbanTripWriter(population).write(outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
