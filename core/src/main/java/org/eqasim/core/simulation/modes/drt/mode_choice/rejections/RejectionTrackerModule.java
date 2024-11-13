package org.eqasim.core.simulation.modes.drt.mode_choice.rejections;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class RejectionTrackerModule extends AbstractModule {
    @Override
    public void install() {
        addEventHandlerBinding().to(RejectionTracker.class);
    }

    @Provides
    @Singleton
    RejectionTracker provideRejectionTracker() {
        return new RejectionTracker();
    }
}
