package org.eqasim.core.components.car_pt.routing;

import java.util.Collections;

import org.eqasim.core.components.transit.routing.EnrichedTransitRoutingModule;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.RoutingModule;
import org.matsim.withinday.replanning.identifiers.filter.TransportModeFilter;

import com.google.inject.Provides;
import com.google.inject.name.Named;

public class EqasimCarPtModule extends AbstractModule{
	
	@Override
	public void install() {
		// TODO Auto-generated method stub
		addRoutingModuleBinding("car_pt").to(CarPtRoutingModule.class);
	}
	
	
	@Provides
	
	public CarPtRoutingModule provideCarPtRoutingModule(@Named("car")RoutingModule roadRoutingModule, @Named("pt")RoutingModule ptRoutingModule, Network network) {
		Network carNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(carNetwork, Collections.singleton("car"));
		return new CarPtRoutingModule(roadRoutingModule, ptRoutingModule, carNetwork);
		
	}
	
}
