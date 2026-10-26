package org.eqasim.switzerland.ch_cmdp.routing.pt_cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.*;

/** Private templates are never returned to callers. Output attributes are codec-validated scalars. */
final class PtCachedItinerary {
    final byte[] bytes;
    private final boolean keepTemplate;
    private volatile List<? extends PlanElement> template;
    private volatile boolean decoded;

    PtCachedItinerary(byte[] bytes, boolean keepTemplate) {
        this.bytes = bytes; this.keepTemplate = keepTemplate;
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
}
