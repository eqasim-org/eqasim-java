package org.eqasim.ile_de_france.grand_paris;

import org.matsim.core.controler.AbstractModule;

public class PersonUtilityModule extends AbstractModule {
	@Override
	public void install() {
		addControlerListenerBinding().to(PersonUtilityListener.class);
	}
}
