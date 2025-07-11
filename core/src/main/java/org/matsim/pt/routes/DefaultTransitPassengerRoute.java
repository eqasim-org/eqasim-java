//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.matsim.pt.routes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.IOException;
import java.util.Objects;

public class DefaultTransitPassengerRoute extends AbstractRoute implements TransitPassengerRoute {
    protected static final int NULL_ID = -1;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    public static final String ROUTE_TYPE = "default_pt";
    public double boardingTime;
    public int transitLineIndex;
    public int transitRouteIndex;
    public int accessFacilityIndex;
    public int egressFacilityIndex;
    public DefaultTransitPassengerRoute chainedRoute;

    public double totalPtRoutingCost = Double.NaN;

    DefaultTransitPassengerRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
        this(startLinkId, endLinkId, (Id)null, (Id)null, (Id)null, (Id)null);
    }

    public DefaultTransitPassengerRoute(TransitStopFacility accessFacility, TransitLine line, TransitRoute route, TransitStopFacility egressFacility) {
        this(accessFacility.getLinkId(), egressFacility.getLinkId(), accessFacility.getId(), egressFacility.getId(), line != null ? line.getId() : null, route != null ? route.getId() : null);
    }

    public DefaultTransitPassengerRoute(TransitStopFacility accessFacility, TransitLine line, TransitRoute route, TransitStopFacility egressFacility, DefaultTransitPassengerRoute chainedRoute) {
        this(accessFacility.getLinkId(), egressFacility.getLinkId(), accessFacility.getId(), egressFacility.getId(), line != null ? line.getId() : null, route != null ? route.getId() : null);
        this.chainedRoute = chainedRoute;
    }

    public DefaultTransitPassengerRoute(Id<Link> accessLinkId, Id<Link> egressLinkId, Id<TransitStopFacility> accessFacilityId, Id<TransitStopFacility> egressFacilityId, Id<TransitLine> transitLineId, Id<TransitRoute> transitRouteId) {
        super(accessLinkId, egressLinkId);
        this.boardingTime = Double.NEGATIVE_INFINITY;
        this.chainedRoute = null;
        this.transitLineIndex = Objects.isNull(transitLineId) ? -1 : transitLineId.index();
        this.transitRouteIndex = Objects.isNull(transitRouteId) ? -1 : transitRouteId.index();
        this.accessFacilityIndex = Objects.isNull(accessFacilityId) ? -1 : accessFacilityId.index();
        this.egressFacilityIndex = Objects.isNull(egressFacilityId) ? -1 : egressFacilityId.index();
    }

    private DefaultTransitPassengerRoute(double boardingTime, int accessFacilityIndex, int egressFacilityIndex, int transitLineIndex, int transitRouteIndex, DefaultTransitPassengerRoute chainedRoute) {
        super((Id)null, (Id)null);
        this.boardingTime = Double.NEGATIVE_INFINITY;
        this.chainedRoute = null;
        this.boardingTime = boardingTime;
        this.accessFacilityIndex = accessFacilityIndex;
        this.egressFacilityIndex = egressFacilityIndex;
        this.transitLineIndex = transitLineIndex;
        this.transitRouteIndex = transitRouteIndex;
        this.chainedRoute = chainedRoute;
    }

    public String getRouteType() {
        return "default_pt";
    }

    public String getRouteDescription() {
        try {
            RouteDescription routeDescription = new RouteDescription(this);
            return OBJECT_MAPPER.writeValueAsString(routeDescription);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRouteDescription(String routeDescription) {
        try {
            RouteDescription parsed = (RouteDescription)OBJECT_MAPPER.readValue(routeDescription, RouteDescription.class);
            this.boardingTime = parsed.boardingTime;
            this.accessFacilityIndex = parsed.accessFacilityId == null ? -1 : parsed.accessFacilityId.index();
            this.egressFacilityIndex = parsed.egressFacilityId == null ? -1 : parsed.egressFacilityId.index();
            this.transitLineIndex = parsed.transitLineId == null ? -1 : parsed.transitLineId.index();
            this.transitRouteIndex = parsed.transitRouteId == null ? -1 : parsed.transitRouteId.index();
            this.chainedRoute = createChainedRoutes(parsed.chainedRoute);
            this.totalPtRoutingCost =  parsed.totalPtRoutingCost;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static DefaultTransitPassengerRoute createChainedRoutes(RouteDescription r) {
        if (r == null) {
            return null;
        } else {
            double boardingTime = r.boardingTime;
            int accessFacilityIndex = r.accessFacilityId == null ? -1 : r.accessFacilityId.index();
            int egressFacilityIndex = r.egressFacilityId == null ? -1 : r.egressFacilityId.index();
            int transitLineIndex = r.transitLineId == null ? -1 : r.transitLineId.index();
            int transitRouteIndex = r.transitRouteId == null ? -1 : r.transitRouteId.index();
            return new DefaultTransitPassengerRoute(boardingTime, accessFacilityIndex, egressFacilityIndex, transitLineIndex, transitRouteIndex, createChainedRoutes(r.chainedRoute));
        }
    }

    public OptionalTime getBoardingTime() {
        return asOptionalTime(this.boardingTime);
    }

    public void setBoardingTime(double boardingTime) {
        OptionalTime.assertDefined(boardingTime);
        this.boardingTime = boardingTime;
    }

    public Id<TransitLine> getLineId() {
        return this.transitLineIndex >= 0 ? Id.get(this.transitLineIndex, TransitLine.class) : null;
    }

    public Id<TransitRoute> getRouteId() {
        return this.transitRouteIndex >= 0 ? Id.get(this.transitRouteIndex, TransitRoute.class) : null;
    }

    public Id<TransitStopFacility> getAccessStopId() {
        return this.accessFacilityIndex >= 0 ? Id.get(this.accessFacilityIndex, TransitStopFacility.class) : null;
    }

    public Id<TransitStopFacility> getEgressStopId() {
        return this.egressFacilityIndex >= 0 ? Id.get(this.egressFacilityIndex, TransitStopFacility.class) : null;
    }

    public DefaultTransitPassengerRoute getChainedRoute() {
        return this.chainedRoute;
    }

    public DefaultTransitPassengerRoute clone() {
        DefaultTransitPassengerRoute copy = new DefaultTransitPassengerRoute(this.getStartLinkId(), this.getEndLinkId(), this.getAccessStopId(), this.getEgressStopId(), this.getLineId(), this.getRouteId());
        if (this.chainedRoute != null) {
            copy.chainedRoute = this.chainedRoute.clone();
        }

        copy.setDistance(this.getDistance());
        OptionalTime var10000 = this.getTravelTime();
        Objects.requireNonNull(copy);
        var10000.ifDefined(copy::setTravelTime);
        var10000 = this.getBoardingTime();
        Objects.requireNonNull(copy);
        var10000.ifDefined(copy::setBoardingTime);
        return copy;
    }

    private static final class RouteDescription {
        public double boardingTime = Double.NEGATIVE_INFINITY;
        public Id<TransitLine> transitLineId;
        public Id<TransitRoute> transitRouteId;
        public Id<TransitStopFacility> accessFacilityId;
        public Id<TransitStopFacility> egressFacilityId;
        public RouteDescription chainedRoute;
        public double totalPtRoutingCost;

        public RouteDescription() {
        }

        public RouteDescription(DefaultTransitPassengerRoute r) {
            this.boardingTime = r.boardingTime;
            this.accessFacilityId = r.accessFacilityIndex == -1 ? null : Id.get(r.accessFacilityIndex, TransitStopFacility.class);
            this.egressFacilityId = r.egressFacilityIndex == -1 ? null : Id.get(r.egressFacilityIndex, TransitStopFacility.class);
            this.transitLineId = r.transitLineIndex == -1 ? null : Id.get(r.transitLineIndex, TransitLine.class);
            this.transitRouteId = r.transitRouteIndex == -1 ? null : Id.get(r.transitRouteIndex, TransitRoute.class);
            if (r.chainedRoute != null) {
                this.chainedRoute = new RouteDescription(r.chainedRoute);
            }
            this.totalPtRoutingCost = r.totalPtRoutingCost;

        }

        @JsonProperty("boardingTime")
        public String getBoardingTime() {
            return Time.writeTime(this.boardingTime);
        }

        @JsonProperty("accessFacilityId")
        public String getAccessFacilityId() {
            return this.accessFacilityId == null ? null : this.accessFacilityId.toString();
        }

        @JsonProperty("egressFacilityId")
        public String getEgressFacilityId() {
            return this.egressFacilityId == null ? null : this.egressFacilityId.toString();
        }

        @JsonProperty("transitLineId")
        public String getTransitLineId() {
            return this.transitLineId == null ? null : this.transitLineId.toString();
        }

        @JsonProperty("transitRouteId")
        public String getRouteLineId() {
            return this.transitRouteId == null ? null : this.transitRouteId.toString();
        }

        @JsonProperty("boardingTime")
        public void setBoardingTime(String boardingTime) {
            this.boardingTime = Time.parseOptionalTime(boardingTime).orElse(Double.NEGATIVE_INFINITY);
        }

        @JsonProperty("transitLineId")
        public void setTransitLineId(String transitLineId) {
            this.transitLineId = transitLineId == null ? null : Id.create(transitLineId, TransitLine.class);
        }

        @JsonProperty("transitRouteId")
        public void setRouteLineId(String transitRouteId) {
            this.transitRouteId = transitRouteId == null ? null : Id.create(transitRouteId, TransitRoute.class);
        }

        @JsonProperty("accessFacilityId")
        public void setAccessFacilityId(String accessFacilityId) {
            this.accessFacilityId = accessFacilityId == null ? null : Id.create(accessFacilityId, TransitStopFacility.class);
        }

        @JsonProperty("egressFacilityId")
        public void setEgressFacilityId(String egressFacilityId) {
            this.egressFacilityId = egressFacilityId == null ? null : Id.create(egressFacilityId, TransitStopFacility.class);
        }

        @JsonProperty("chainedRoute")
        @JsonInclude(Include.NON_NULL)
        public RouteDescription getChainedRoute() {
            return this.chainedRoute;
        }

        @JsonProperty("chainedRoute")
        public void setChainedRoute(RouteDescription chainedRoute) {
            this.chainedRoute = chainedRoute;
        }

        @JsonProperty("totalPtRoutingCost")
        public String getTotalPtRoutingCost() {
            return String.valueOf(totalPtRoutingCost);
        }

        @JsonProperty("totalPtRoutingCost")
        public void setTotalPtRoutingCost(String totalPtRoutingCost) {
            this.totalPtRoutingCost = Double.parseDouble(totalPtRoutingCost);
        }
    }
}
