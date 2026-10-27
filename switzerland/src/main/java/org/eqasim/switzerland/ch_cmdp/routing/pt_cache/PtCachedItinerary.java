package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;
import org.matsim.pt.routes.TransitPassengerRoute;

/** Private templates are never returned to callers. Output attributes are codec-validated scalars. */
final class PtCachedItinerary {
    final byte[] bytes;
    final double departureTime;
    private final boolean keepTemplate;
    private volatile List<? extends PlanElement> template;
    private volatile boolean decoded;

    PtCachedItinerary(byte[] bytes, boolean keepTemplate, double departureTime) {
        this.bytes = bytes; this.keepTemplate = keepTemplate; this.departureTime = departureTime;
    }

    // Reserve estimated space for both binary payload and decoded template before admitting a record.
    long estimatedBytes() { return 512L + 4L * bytes.length; }

    List<? extends PlanElement> restore(PopulationFactory factory) throws IOException {
        if (!keepTemplate) return PtItineraryCodec.decode(bytes, factory);
        if (!decoded) synchronized (this) {
            if (!decoded) { template = PtItineraryCodec.decode(bytes, factory); decoded = true; }
        }
        if (template == null) return null;
        var result = new ArrayList<PlanElement>(template.size());
        for (var element : template) {
            PlanElement copy;
            if (element instanceof Leg leg) {
                var target = factory.createLeg(leg.getMode());
                target.setRoutingMode(leg.getRoutingMode());
                if (leg.getDepartureTime().isDefined()) target.setDepartureTime(leg.getDepartureTime().seconds());
                if (leg.getTravelTime().isDefined()) target.setTravelTime(leg.getTravelTime().seconds());
                if (leg.getRoute() != null) target.setRoute(leg.getRoute().clone());
                copy = target;
            } else {
                var activity = (Activity) element;
                Coord coord = activity.getCoord();
                Coord copiedCoord = coord == null ? null : coord.hasZ()
                        ? new Coord(coord.getX(), coord.getY(), coord.getZ()) : new Coord(coord.getX(), coord.getY());
                var target = factory.createActivityFromCoord(activity.getType(), copiedCoord);
                target.setLinkId(activity.getLinkId()); target.setFacilityId(activity.getFacilityId());
                if (activity.getStartTime().isDefined()) target.setStartTime(activity.getStartTime().seconds());
                if (activity.getEndTime().isDefined()) target.setEndTime(activity.getEndTime().seconds());
                if (activity.getMaximumDuration().isDefined()) target.setMaximumDuration(activity.getMaximumDuration().seconds());
                copy = target;
            }
            element.getAttributes().getAsMap().forEach(copy.getAttributes()::putAttribute);
            result.add(copy);
        }
        return result;
    }

    /** Adjust a fresh copy only. Scheduled PT boarding/arrival times and downstream transfers stay fixed. */
    static boolean retime(List<? extends PlanElement> elements, double delta) {
        if (delta == 0 || elements == null || elements.isEmpty()) return true;
        for (var element : elements) {
            if (element instanceof Leg leg) {
                if (leg.getRoute() instanceof TransitPassengerRoute route) {
                    if (!route.getBoardingTime().isDefined() || !leg.getDepartureTime().isDefined()
                            || !leg.getTravelTime().isDefined()) return false;
                    double departure = leg.getDepartureTime().seconds() + delta;
                    if (departure > route.getBoardingTime().seconds() || leg.getTravelTime().seconds() < delta) return false;
                    leg.setDepartureTime(departure);
                    leg.setTravelTime(leg.getTravelTime().seconds() - delta);
                    if (route.getTravelTime().isDefined()) route.setTravelTime(route.getTravelTime().seconds() - delta);
                    return true;
                }
                if (leg.getDepartureTime().isDefined()) leg.setDepartureTime(leg.getDepartureTime().seconds() + delta);
            } else if (element instanceof Activity activity) {
                if (activity.getStartTime().isDefined()) activity.setStartTime(activity.getStartTime().seconds() + delta);
                if (activity.getEndTime().isDefined()) activity.setEndTime(activity.getEndTime().seconds() + delta);
            }
        }
        return true; // Pure walking: shift absolute times, keeping all durations unchanged.
    }
}
