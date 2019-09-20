package org.eqasim.core.simulation.analysis;

import org.matsim.core.controler.AbstractModule;

public class EqasimAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(AnalysisOutputListener.class);
	}
}
