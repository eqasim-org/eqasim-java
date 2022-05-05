package org.eqasim.ile_de_france.analysis.delay;

import org.matsim.core.controler.AbstractModule;

public class DelayAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(DelayAnalysisListener.class);
	}
}
