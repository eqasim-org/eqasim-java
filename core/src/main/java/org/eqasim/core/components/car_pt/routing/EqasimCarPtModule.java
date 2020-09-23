package org.eqasim.core.components.car_pt.routing;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoutingModule;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Provides;

public class EqasimCarPtModule extends AbstractModule{
	
	@Override
	public void install() {
		// TODO Auto-generated method stub
		addRoutingModuleBinding("car_pt").to(CarPtRoutingModule.class);
	}
	
	
	@Provides
	public CarPtRoutingModule provideCarPtRoutingModule(PopulationFactory populationFactory, RoutingModule walkRoutingModule, 
			boolean useAccessEgress, RoutingModule roadRoutingModule, EnrichedTransitRoutingModule ptRoutingModule) {
		
		return new CarPtRoutingModule(populationFactory, walkRoutingModule,	useAccessEgress, roadRoutingModule, ptRoutingModule);
		
	}
	
}
