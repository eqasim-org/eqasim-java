package org.eqasim.core.components;

import org.eqasim.core.components.travel_disutility.EqasimTravelDisutilityFactory;
import org.eqasim.core.simulation.policies.routing.RoutingPenalty;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.RoutingModeMainModeIdentifier;

import com.google.inject.Provides;
import com.google.inject.Singleton;

public class EqasimComponentsModule extends AbstractModule {
	@Override
	public void install() {
		bind(MainModeIdentifier.class).to(RoutingModeMainModeIdentifier.class);

		// OnlyTimeDependentTravelDisutilityFactory
		//addTravelDisutilityFactoryBinding(TransportMode.car).to(EqasimTravelDisutilityFactory.class);
		//addTravelDisutilityFactoryBinding("car_passenger").to(EqasimTravelDisutilityFactory.class);
		//addTravelDisutilityFactoryBinding("truck").to(EqasimTravelDisutilityFactory.class);

	}

	//@Provides
	//@Singleton
	//EqasimTravelDisutilityFactory providePolicyTravelDisutilityFactory(RoutingPenalty linkPenalty) {
	//	return new EqasimTravelDisutilityFactory(linkPenalty);
	//}
}
