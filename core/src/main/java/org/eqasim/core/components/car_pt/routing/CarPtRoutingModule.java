package org.eqasim.core.components.car_pt.routing;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;


import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;

public class CarPtRoutingModule implements RoutingModule{
	
	private final RoutingModule carRoutingModule;
	private final Network network;

	//Create an object of a ptRoutingModule
	private final RoutingModule ptRoutingModule;

	//@Inject
	public CarPtRoutingModule(RoutingModule roadRoutingModule, RoutingModule ptRoutingModule, Network network) {
		this.carRoutingModule = roadRoutingModule;
		this.ptRoutingModule = ptRoutingModule;
		this.network = network;
				
	}

	@Override
	/*
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
*/
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		
		double X = 695217.09, Y = 7059186.19;
		
		
		/*
		double Lat = 50.62964, Long = 2.93251586; 	X = 695217.09, Y = 7059186.19;
		50.6819458	2.878044; 						X = 691365.43, Y = 7065019.42;
		50.6183128	3.049986						X = 703543.53, Y = 7057923.10
		50.6080246	3.03906918						X = 702770.20, Y = 7056776.29
		50.5517769	2.914486						X = 693929.84, Y = 7050511.72
		50.6175	3.06391								X = 704530.69, Y = 7057833.24
		50.6500053	3.12651539						X = 708963.08, Y = 7061460.64
		50.71675	3.166937						X = 711811.05, Y = 7068903.84
		50.5277328	2.80166936						X = 685914.90, Y = 7047847.26
		50.73656	3.17222							X = 712180.02, Y = 7071112.37
		50.54696	3.032925						X = 702337.39, Y = 7049972.24
		50.60485	3.139705						X = 709906.41, Y = 7056430.63
		*/
		
		 Coord prCoord = new Coord(X, Y); // XY coords of some "test" P&R station somewhere in your scenario. Later you'll want to choose one specifically
		 
		 //Link prLink = NetworkUtils.getClosestLink(prCoord); // getClosestLink method not exist
		  
		 Link prLink = NetworkUtils.getNearestLink(network, prCoord);
		 
		 Facility prFacility = new LinkWrapperFacility(prLink);


		 // Here you create a car trip to the PR facility
		 List<? extends PlanElement> carElements = carRoutingModule.calcRoute(fromFacility, prFacility, departureTime, null);
		 
		 //double vehicleDistance = Double.NaN;
		 double vehicleTravelTime = Double.NaN;
		 //double price = Double.NaN;
		 
		 Leg leg = (Leg) carElements.get(0);
		 //vehicleDistance = leg.getRoute().getDistance();
		 vehicleTravelTime = leg.getRoute().getTravelTime(); // can not invoke seconds() in this context
		 
		// Given the request time, we can calculate the waiting time
		double timeToAccessPt = 600; //We take 10 min to park the car and access to PT
		
		double ptDepartureTime = departureTime + vehicleTravelTime + timeToAccessPt;

		 // Here you create a PT trip from the PR facility to the destination
		 List<? extends PlanElement> ptElements = ptRoutingModule.calcRoute(prFacility, toFacility, ptDepartureTime , person);

		 //Creation interaction between car and pt
		 Activity interactionActivtyCarPt = PopulationUtils.createActivityFromCoordAndLinkId("carPt interaction", prCoord, prLink.getId());
		 
		 // Create full trip
		 List<PlanElement> allElements = new LinkedList<>();
		 allElements.addAll(carElements);
		 allElements.add(interactionActivtyCarPt);
		 allElements.addAll(ptElements);
		 
		 
		 return allElements;
	
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		
		return new StageActivityTypesImpl("pt interaction","carPt interaction");
	}

}
