package org.eqasim.core.simulation.modes.feeder_drt.analysis;

import com.google.inject.Singleton;
import org.matsim.core.controler.AbstractModule;

public class FeederDrtAnalysisModule extends AbstractModule {

    @Override
    public void install() {
        bind(FeederDrtAnalysisListener.class).in(Singleton.class);
        addControlerListenerBinding().to(FeederDrtAnalysisListener.class);
    }
}
