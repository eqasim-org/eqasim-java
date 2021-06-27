package org.eqasim.examples.corsica_drt.analysis;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;

public class DvrpAnalsisModule extends AbstractModule {
	@Override
	public void install() {
		bind(DvrpAnalysisListener.class).in(Singleton.class);
		addControlerListenerBinding().to(DvrpAnalysisListener.class);
	}
}
