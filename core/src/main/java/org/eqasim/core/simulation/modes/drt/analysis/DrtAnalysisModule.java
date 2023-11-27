package org.eqasim.core.simulation.modes.drt.analysis;

import com.google.inject.Singleton;
import org.eqasim.core.simulation.analysis.DrtAnalysisListener;
import org.matsim.core.controler.AbstractModule;

public class DrtAnalysisModule extends AbstractModule {
	@Override
	public void install() {
		bind(DrtAnalysisListener.class).in(Singleton.class);
		addControlerListenerBinding().to(DrtAnalysisListener.class);
	}
}
