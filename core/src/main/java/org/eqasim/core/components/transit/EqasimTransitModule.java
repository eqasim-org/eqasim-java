package org.eqasim.core.components.transit;

import org.eqasim.core.components.transit.departure.DefaultDepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.matsim.core.controler.AbstractModule;
import org.matsim.pt.router.TransitRouter;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;

public class EqasimTransitModule extends AbstractModule {
	@Override
	public void install() {
		bind(TransitRouter.class).to(SwissRailRaptor.class);

		bind(DepartureFinder.class).to(DefaultDepartureFinder.class);
	}
}
