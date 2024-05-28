package org.eqasim.ile_de_france.policies;


import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.router.RoutingModule;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class CarPTRouterProvider implements Provider<RoutingModule>{

    private final String routingMode;
    private final String mode;

	@Inject
    @Named(TransportMode.pt)
    private Provider<RoutingModule> ptRouterProvider;
	@Inject
	@Named(TransportMode.car)
	private Provider<RoutingModule> carRouterProvider;
    @Inject SwissRailRaptor raptor;

   public CarPTRouterProvider() {
        this("car_pt", "car_pt");
    }

    public CarPTRouterProvider(String mode, String routingMode) {
        this.mode = "car_pt";
        this.routingMode = "car_pt";
    }


    @Override
    public RoutingModule get(){
		SwissRailRaptorData data = raptor.getUnderlyingData();
		return new CarPTRouter(carRouterProvider.get(), ptRouterProvider.get(), data);
    }


}
