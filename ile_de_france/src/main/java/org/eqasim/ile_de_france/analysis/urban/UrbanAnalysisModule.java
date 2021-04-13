package org.eqasim.ile_de_france.analysis.urban;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;

public class UrbanAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(UrbanTripListener.class);
	}

	@Provides
	public UrbanTripListener provideUrbanTripListener(Population population, OutputDirectoryHierarchy outputHierarchy) {
		return new UrbanTripListener(population, outputHierarchy);
	}
}
