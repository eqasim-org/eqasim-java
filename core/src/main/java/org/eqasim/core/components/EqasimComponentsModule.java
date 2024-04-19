package org.eqasim.core.components;

import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;

public class EqasimComponentsModule extends AbstractModule {
	@Override
	public void install() {
		bind(MainModeIdentifier.class).to(RoutingModeMainModeIdentifier.class);
	}
}
