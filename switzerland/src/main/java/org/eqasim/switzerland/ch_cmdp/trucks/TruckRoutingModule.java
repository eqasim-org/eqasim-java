package org.eqasim.switzerland.ch_cmdp.trucks;

import com.google.inject.Singleton;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;

/** Install after road overrides: share car travel times and calculate truck costs independently. */
public final class TruckRoutingModule extends AbstractModule {
    @Override public void install() {
        addTravelTimeBinding(TransportMode.truck).to(TruckTravelTime.class).in(Singleton.class);
        addTravelDisutilityFactoryBinding(TransportMode.truck).to(TruckTravelDisutilityFactory.class).in(Singleton.class);
    }
}
