package org.eqasim.jakarta.roadpricing;


import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.eqasim.jakarta.roadpricing.JakartaMcCalcAverageTolledTripLength;
//import org.eqasim.jakarta.roadpricing.JakartaMcCalcPaidToll;
//import org.eqasim.jakarta.roadpricing.JakartaMcRoadPricingScheme;
import org.eqasim.jakarta.roadpricing.JakartaMcControlerDefaultsWithRoadPricingModule.RoadPricingInitializer;
import org.eqasim.jakarta.roadpricing.JakartaMcControlerDefaultsWithRoadPricingModule.RoadPricingSchemeProvider;
import org.eqasim.jakarta.roadpricing.JakartaMcControlerDefaultsWithRoadPricingModule.TravelDisutilityIncludingTollFactoryProvider;

import com.google.inject.Singleton;

public final class JakartaMcRoadPricingModule extends AbstractModule {
	
	private JakartaMcRoadPricingScheme scheme;

	public JakartaMcRoadPricingModule() {
		
	}
	
	public JakartaMcRoadPricingModule( JakartaMcRoadPricingScheme scheme ) {
		this.scheme = scheme;
		
	}
	
	@Override
	public void install() {
		ConfigUtils.addOrGetModule(getConfig(), JakartaMcRoadPricingConfigGroup.GROUP_NAME, JakartaMcRoadPricingConfigGroup.class);
		
		
		// TODO sort out different ways to set toll schemes; reduce automagic 
		if ( scheme != null) {
			// scheme has come in from the constructor, use that one:
			bind(JakartaMcRoadPricingScheme.class).toInstance(scheme);
		} else {
			// no scheme has come in from the constructor, use a class that reads it from file:
			bind(JakartaMcRoadPricingScheme.class).toProvider(RoadPricingSchemeProvider.class).in(Singleton.class);
		}
		// also add RoadPricingScheme as ScenarioElement.  yyyy TODO might try to get rid of this; binding it is safer
		bind(RoadPricingInitializer.class).asEagerSingleton();
		
		// add the toll to the routing disutility.  also includes "randomizing":
		addTravelDisutilityFactoryBinding(TransportMode.motorcycle).toProvider(TravelDisutilityIncludingTollFactoryProvider.class).asEagerSingleton();

		// specific re-routing strategy for area toll:
		// yyyy TODO could probably combine them somewhat
		bind(JakartaMcPlansCalcRouteWithTollOrNot.class);
		addPlanStrategyBinding("ReRouteAreaToll").toProvider(JakartaMcReRouteAreaToll.class);
		addTravelDisutilityFactoryBinding("motorcycle_with_payed_area_toll").toInstance(new RandomizingTimeDistanceTravelDisutilityFactory(TransportMode.motorcycle, getConfig().planCalcScore()));
		addRoutingModuleBinding("motorcycle_with_payed_area_toll").toProvider(new JakartaMcRoadPricingNetworkRouting());
		
		// yyyy TODO It might be possible that the area stuff is adequatly resolved by the randomizing approach.  Would need to try 
		// that out.  kai, sep'16

		// this is what makes the mobsim compute tolls and generate money events
		// TODO yyyy could probably combine the following two:
		addControlerListenerBinding().to(JakartaMcRoadPricingControlerListener.class);
		bind(JakartaMcCalcPaidToll.class).in(Singleton.class);

		// this is for analysis only:
		bind(JakartaMcCalcAverageTolledTripLength.class).in(Singleton.class);
	}
}