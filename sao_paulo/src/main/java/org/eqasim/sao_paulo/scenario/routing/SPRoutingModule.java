package org.eqasim.sao_paulo.scenario.routing;

import org.eqasim.sao_paulo.scenario.routing.taxi.TaxiRoutingModuleProvider;
import org.matsim.core.controler.AbstractModule;

public class SPRoutingModule extends AbstractModule{

	@Override
	public void install() {
		addRoutingModuleBinding("taxi").toProvider(TaxiRoutingModuleProvider.class);
	}

}