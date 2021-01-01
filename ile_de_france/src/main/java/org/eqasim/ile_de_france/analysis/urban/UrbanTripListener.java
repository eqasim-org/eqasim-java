package org.eqasim.ile_de_france.analysis.urban;

import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.StageActivityTypes;

public class UrbanTripListener implements StartupListener {
	private final Population population;
	private final StageActivityTypes stageActivityTypes;
	private final OutputDirectoryHierarchy outputHierarchy;

	public UrbanTripListener(Population population, StageActivityTypes stageActivityTypes,
			OutputDirectoryHierarchy outputHierarchy) {
		this.population = population;
		this.stageActivityTypes = stageActivityTypes;
		this.outputHierarchy = outputHierarchy;
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		try {
			String outputPath = outputHierarchy.getOutputFilename("urban.csv");
			new UrbanTripWriter(population, stageActivityTypes).write(outputPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
