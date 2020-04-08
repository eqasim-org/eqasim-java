package org.eqasim.examples.zurich_adpt.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;

public class AdPTRoute extends AbstractRoute {
	final static String AdPT_ROUTE = "adpt";

	private double inVehicleTime;
	public AdPTRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	@Override
	public String getRouteDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		// TODO Auto-generated method stub
		
	}
	
	public double getInVehicleTime() {
		return inVehicleTime;
	}

	public void setInVehicleTime(double inVehicleTime) {
		this.inVehicleTime = inVehicleTime;
	}

	@Override
	public String getRouteType() {
		return this.AdPT_ROUTE;
	}

}
