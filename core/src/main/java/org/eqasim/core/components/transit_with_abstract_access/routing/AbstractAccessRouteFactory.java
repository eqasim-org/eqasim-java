package org.eqasim.core.components.transit_with_abstract_access.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.RouteFactory;

public class AbstractAccessRouteFactory implements RouteFactory {
    @Override
    public Route createRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
        return new DefaultAbstractAccessRoute(startLinkId, endLinkId);
    }

    @Override
    public String getCreatedRouteType() {
        return "abstractAccess";
    }
}
