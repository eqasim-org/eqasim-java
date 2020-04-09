package org.eqasim.examples.zurich_adpt.scenario;

import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.examples.zurich_adpt.AdPTModeAvailability;
import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.name.Named;

public class AdPTModule extends AbstractEqasimExtension {
	final static public String ADPT_MODE = "adpt";

	static public final String ADPT_MODE_AVAILABILITY_NAME = "AdPTModeAvailability";
	final private Zones zones;
	final private ZonalVariables zonalVariables;
	public AdPTModule(Zones zones, ZonalVariables zonalVariables) {
		this.zones = zones;
		this.zonalVariables = zonalVariables;
	}

	@Override
	protected void installEqasimExtension() {
		bindModeAvailability(ADPT_MODE_AVAILABILITY_NAME).to(AdPTModeAvailability.class);
		addRoutingModuleBinding(ADPT_MODE).to(AdPTRoutingModule.class);
		bind(Zones.class).toInstance(zones);
		bind(ZonalVariables.class).toInstance(zonalVariables);
	}

	@Provides
	public AdPTRoutingModule provideAdPTRoutingModule(@Named("walk") RoutingModule walkRoutingModule,
			@Named("car") Provider<RoutingModule> roadRoutingModuleProvider, PopulationFactory populationFactory,
			AdPTRouteFactory routeFactory, @Named("car") Network network) {

		return new AdPTRoutingModule(walkRoutingModule, roadRoutingModuleProvider.get(), this.zones, network, routeFactory,
				populationFactory);
	}

}
