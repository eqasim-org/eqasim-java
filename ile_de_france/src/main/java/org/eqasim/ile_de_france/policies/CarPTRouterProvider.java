package org.eqasim.ile_de_france.policies;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.MultimodalLinkChooser;
import org.matsim.core.router.NetworkRoutingInclAccessEgressModule;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.SingleModeNetworksCache;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.pt.router.TransitRouter;

import com.google.inject.Key;
import com.google.inject.Provides;
import javax.inject.Provider;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorCore;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorFactory;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorRoutingModule;

public class CarPTRouterProvider implements Provider<RoutingModule>{

    private final String routingMode;
    private final String mode;

    @Inject Map<String, TravelTime> travelTimes;
	@Inject Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	@Inject SingleModeNetworksCache singleModeNetworksCache;
	@Inject PlansCalcRouteConfigGroup plansCalcRouteConfigGroup;
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
