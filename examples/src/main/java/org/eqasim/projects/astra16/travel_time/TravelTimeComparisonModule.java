package org.eqasim.projects.astra16.travel_time;

import org.eqasim.projects.astra16.convergence.AstraConvergenceCriterion;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class TravelTimeComparisonModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(TravelTimeComparisonListener.class);
		addEventHandlerBinding().to(TravelTimeComparisonListener.class);
	}

	@Provides
	@Singleton
	public TravelTimeComparisonListener provideTravelTimeComparisonListener(OutputDirectoryHierarchy outputHierarchy,
			Population population, AstraConvergenceCriterion criterion) {
		return new TravelTimeComparisonListener(outputHierarchy, population, criterion);
	}
}
