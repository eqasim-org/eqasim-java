package org.eqasim.core.components.car_pt.routing;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.core.car_pt.data.CarPtOperator;
import org.eqasim.core.components.transit.routing.EnrichedTransitRoutingModule;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;

import com.google.inject.Inject;

import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class CarPtRoutingModule implements RoutingModule{
	
	//private final AVOperatorChoiceStrategy choiceStrategy;
	//private final AVRouteFactory routeFactory;
	private final RoutingModule walkRoutingModule;
	private final PopulationFactory populationFactory;
	private final RoutingModule carRoutingModule;
	//private final PriceCalculator priceCalculator;

	//Create an object of a ptRoutingModule
	private final EnrichedTransitRoutingModule ptRoutingModule;
	private final boolean useAccessEgress;

	//@Inject
	public CarPtRoutingModule(PopulationFactory populationFactory, RoutingModule walkRoutingModule, 
			boolean useAccessEgress, RoutingModule roadRoutingModule, EnrichedTransitRoutingModule ptRoutingModule) {
		this.walkRoutingModule = walkRoutingModule;
		this.populationFactory = populationFactory;
		this.useAccessEgress = useAccessEgress;
		this.carRoutingModule = roadRoutingModule;
		this.ptRoutingModule = ptRoutingModule;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		//Id<AVOperator> operatorId = choiceStrategy.chooseRandomOperator();
		//return calcRoute(fromFacility, toFacility, departureTime, person, operatorId);
		
		Leg leg = PopulationUtils.createLeg("car_pt");
		leg.setTravelTime(600.0);
		
		Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		route.setTravelTime(600.0);
		route.setDistance(100.0);
		
		leg.setRoute(route);
		
		return Collections.singletonList(leg);
	}

	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person, Network network) {
		
		double X = 50.62964, Y = 2.93251586;
		/*
		50.6819458	2.878044
		//50.6183128	3.049986
		//50.6080246	3.03906918
		50.5517769	2.914486
		50.6175	3.06391
		50.6500053	3.12651539
		50.71675	3.166937
		50.5277328	2.80166936
		50.73656	3.17222
		50.54696	3.032925
		50.60485	3.139705
		*/
		
		 Coord prCoord = new Coord(X, Y); // XY coords of some "test" P&R station somewhere in your scenario. Later you'll want to choose one specifically
		 
		 //Link prLink = NetworkUtils.getClosestLink(prCoord); // getClosestLink method not exist
		  
		 Link prLink = NetworkUtils.getNearestLink(network, prCoord);
		 
		 Facility prFacility = new LinkWrapperFacility(prLink);


		 // Here you create a car trip to the PR facility
		 List<? extends PlanElement> carElements = carRoutingModule.calcRoute(fromFacility, prFacility, departureTime, null);
		 
		 double vehicleDistance = Double.NaN;
		 double vehicleTravelTime = Double.NaN;
		 double price = Double.NaN;
		 
		 Leg leg = (Leg) carElements.get(0);
		 vehicleDistance = leg.getRoute().getDistance();
		 vehicleTravelTime = leg.getRoute().getTravelTime(); // can not invoke seconds() in this context
		 
		// Given the request time, we can calculate the waiting time
		double timeToAccessPt = 600; //We take 10 min to park the car and access to PT
		
		double ptDepartureTime = vehicleTravelTime + timeToAccessPt;

		 // Here you create a PT trip from the PR facility to the destination
		 List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(prFacility, toFacility, ptDepartureTime , person);

		 // Create full trip
		 List<PlanElement> allElements = new LinkedList<>();
		 allElements.addAll(carElements);
		 allElements.addAll(ptElements);

		 return allElements;
	
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl();
	}

}
