package org.eqasim.components.transit.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class DefaultEnrichedTransitRouteFactory implements RouteFactory {
	@Override
	public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		return new DefaultEnrichedTransitRoute(startLinkId, endLinkId);
	}

	@Override
	public String getCreatedRouteType() {
		return DefaultEnrichedTransitRoute.ROUTE_TYPE;
	}
}
