package org.eqasim.examples.zurich_adpt.scenario;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.RouteFactory;

import com.google.inject.Singleton;

@Singleton
public class AdPTRouteFactory implements RouteFactory {
	@Override
	public AdPTRoute createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		return new AdPTRoute(startLinkId, endLinkId);
	}

	@Override
	public String getCreatedRouteType() {
		return AdPTRoute.AdPT_ROUTE;
	}
}
