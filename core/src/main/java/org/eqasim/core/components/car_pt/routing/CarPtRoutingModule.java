package org.eqasim.core.components.car_pt.routing;

import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.router.RoutingModule;
import org.matsim.facilities.Facility;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;

public class CarPtRoutingModule implements RoutingModule{
	
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		
		Leg leg = PopulationUtils.createLeg("car_pt");
		leg.setTravelTime(600.0);
		
		Route route = new GenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
		route.setTravelTime(600.0);
		route.setDistance(100.0);
		
		leg.setRoute(route);
		
		return Collections.singletonList(leg);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		return new StageActivityTypesImpl();
	}

}
