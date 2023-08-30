package org.eqasim.core.components.transit_with_abstract_access.routing;

import org.eqasim.core.components.transit_with_abstract_access.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;


public class DefaultAbstractAccessRoute extends AbstractRoute implements AbstractAccessRoute{

    public static final String ROUTE_TYPE = "abstractAccess";

    private final AbstractAccessItem accessItem;
    private final boolean leavingAccessCenter;

    public DefaultAbstractAccessRoute(Id<Link> startLinkId, Id<Link> endLinkId, AbstractAccessItem accessItem) {
        super(startLinkId, endLinkId);
        this.accessItem = accessItem;
        Id<Link> accessLink = this.accessItem.getCenterStop().getLinkId();
        if(accessLink.equals(startLinkId)) {
            this.leavingAccessCenter = true;
        } else if(accessLink.equals(endLinkId)) {
            this.leavingAccessCenter = false;
        } else {
            throw new IllegalStateException("Supplied accessItem should have a center located on one of startLinkId or endLinkId");
        }
    }

    @Override
    public String getRouteDescription() {
        return null;
    }

    @Override
    public void setRouteDescription(String routeDescription) {

    }
    @Override
    public String getRouteType() {
        return ROUTE_TYPE;
    }

    @Override
    public AbstractAccessItem getAbstractAccessItem() {
        return this.accessItem;
    }

    public boolean isLeavingAccessCenter() {
        return this.leavingAccessCenter;
    }
}
