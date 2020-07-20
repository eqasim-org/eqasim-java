package org.eqasim.ile_de_france.grand_paris;

import java.io.File;
import java.io.IOException;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PersonUtilityListener implements ShutdownListener {
	private final OutputDirectoryHierarchy outputHierarchy;
	private final Population population;

	@Inject
	public PersonUtilityListener(OutputDirectoryHierarchy outputHierarchy, Population population) {
		this.outputHierarchy = outputHierarchy;
		this.population = population;
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		File path = new File(outputHierarchy.getOutputFilename("person_utiltiies.csv"));
		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

		try {
			new PersonUtilityWriter(population, stageActivityTypes).writeFile(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
