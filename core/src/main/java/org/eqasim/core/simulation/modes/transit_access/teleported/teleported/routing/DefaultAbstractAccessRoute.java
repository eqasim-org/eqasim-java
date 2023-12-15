package org.eqasim.core.simulation.modes.transit_access.teleported.teleported.routing;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eqasim.core.simulation.modes.transit_access.teleported.teleported.abstract_access.AbstractAccessItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;

import java.io.IOException;


public class DefaultAbstractAccessRoute extends AbstractRoute implements AbstractAccessRoute{

    public static final String ROUTE_TYPE = "abstractAccess";

    private RouteDescription routeDescription;

    public DefaultAbstractAccessRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
        super(startLinkId, endLinkId);
    }

    public DefaultAbstractAccessRoute(Id<Link> startLinkId, Id<Link> endLinkId, AbstractAccessItem accessItem) {
        super(startLinkId, endLinkId);
        this.routeDescription = new RouteDescription();
        routeDescription.accessItemId = accessItem.getId();
        routeDescription.isRouted = accessItem.isUsingRoutedDistance();
        Id<Link> accessLink = accessItem.getCenterStop().getLinkId();
        if(accessLink.equals(startLinkId)) {
            this.routeDescription.leavingAccessCenter = true;
        } else if(accessLink.equals(endLinkId)) {
            this.routeDescription.leavingAccessCenter = false;
        } else {
            throw new IllegalStateException("Supplied accessItem should have a center located on one of startLinkId or endLinkId");
        }
    }

    @Override
    public String getRouteDescription() {
        try {
            return new ObjectMapper().writeValueAsString(routeDescription);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void setRouteDescription(String routeDescription) {
        try {
            this.routeDescription = new ObjectMapper().readValue(routeDescription, RouteDescription.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public String getRouteType() {
        return ROUTE_TYPE;
    }

    public boolean isLeavingAccessCenter() {
        return this.routeDescription.leavingAccessCenter;
    }

    @Override
    public boolean isRouted() {
        return this.routeDescription.isRouted;
    }

    @Override
    public double getWaitTime() {
        return routeDescription.waitTime;
    }

    public void setWaitTime(double waitTime) {
        this.routeDescription.waitTime = waitTime;
    }

    @Override
    public Id<AbstractAccessItem> getAbstractAccessItemId() {
        return this.routeDescription.accessItemId;
    }

    public static class RouteDescription {
        public Id<AbstractAccessItem> accessItemId;
        public boolean leavingAccessCenter;
        public boolean isRouted;
        public double waitTime = 0;

        @JsonProperty("accessItemId")
        public String getAccessItemId() {
            return accessItemId == null ? null : accessItemId.toString();
        }

        @JsonProperty("accessItemId")
        public void setAccessItemId(String accessItemId) {
            this.accessItemId = accessItemId == null ? null : Id.create(accessItemId, AbstractAccessItem.class);
        }
    }
}
