package org.eqasim.ile_de_france.policies;

import java.util.Map;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;

import com.google.inject.Provider;
import com.google.inject.name.Named;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class CarPTRouterProvider implements Provider<RoutingModule>{

    private final String routingMode;
    private final String mode;

    @Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject SingleModeNetworksCache singleModeNetworksCache;
	@Inject RoutingConfigGroup plansCalcRouteConfigGroup;
	@Inject Network network;
	@Inject NetworkConfigGroup networkConfigGroup;
	@Inject PopulationFactory populationFactory;
	@Inject LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	@Inject Scenario scenario ;
	@Inject TimeInterpretation timeInterpretation;
	@Inject MultimodalLinkChooser multimodalLinkChooser;
	@Inject
	@Named(TransportMode.walk)
	RoutingModule walkRouter;
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
        this.mode = mode;
        this.routingMode = routingMode;
    }


    @Override
    public RoutingModule get(){
		SwissRailRaptorData data = raptor.getUnderlyingData();
		return new CarPTRouter(carRouterProvider.get(), ptRouterProvider.get(), data);
    }


}
