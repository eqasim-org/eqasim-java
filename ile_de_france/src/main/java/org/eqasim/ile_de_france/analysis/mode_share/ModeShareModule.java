package org.eqasim.ile_de_france.analysis.mode_share;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.router.MainModeIdentifier;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class ModeShareModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(ModeShareCriterion.class);
	}

	@Provides
	@Singleton
	ModeShareCriterion provideModeShareCriterion(MainModeIdentifier mainModeIdentifier, Population population,
			OutputDirectoryHierarchy outputHierarchy) {
		return new ModeShareCriterion(mainModeIdentifier, population, outputHierarchy);
	}
}
