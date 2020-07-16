package org.eqasim.san_francisco.bike;


import org.eqasim.san_francisco.bike.routing.SanFranciscoBikeLinkSpeedCalculator;
import org.eqasim.san_francisco.bike.routing.SanFranciscoBikeTravelDisutilityFactory;
import org.eqasim.san_francisco.bike.routing.SanFranciscoBikeTravelTime;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;

public final class SanFranciscoBikeModule extends AbstractModule {

    public SanFranciscoBikeModule() {
    }

    @Override
    public void install() {
        addTravelTimeBinding(TransportMode.bike).to(SanFranciscoBikeTravelTime.class).asEagerSingleton();
        addTravelDisutilityFactoryBinding(TransportMode.bike).to(SanFranciscoBikeTravelDisutilityFactory.class).asEagerSingleton();
        bind(SanFranciscoBikeLinkSpeedCalculator.class).asEagerSingleton();
    }

}
