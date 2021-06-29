package org.eqasim.ile_de_france.analysis.stuck;

import org.matsim.core.controler.AbstractModule;

public class StuckAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(StuckAnalysisListener.class);
	}
}
