package org.eqasim.examples.zurich_adpt.scenario;

import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class AdPTModule extends AbstractModule {
	final static public String ADPT_MODE = "adpt";

	@Override
	public void install() {

		addRoutingModuleBinding(ADPT_MODE).to(AdPTRoutingModule.class);

	}

	@Provides
	public AdPTRoutingModule provideAdPTRoutingModule(@Named("walk") RoutingModule walkRoutingModule,
			@Named("car") Provider<RoutingModule> roadRoutingModuleProvider, PopulationFactory populationFactory,
			AdPTRouteFactory routeFactory, Zones zones, @Named("car")Network network) {

		return new AdPTRoutingModule(walkRoutingModule, roadRoutingModuleProvider.get(),
				zones, network, routeFactory, populationFactory);
	}

}
