package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import java.util.Collection;
import java.util.Random;

/** One lightweight, reusable spatial index for activity-facility relocation. */
final class CrossBorderFacilityIndex {
    private static final double BOUNDS_PADDING_METERS = 1.0;

    private final QuadTree<ActivityFacility> facilities;

    CrossBorderFacilityIndex(ActivityFacilities activityFacilities, Network network) {
        Bounds bounds = Bounds.from(activityFacilities, network);
        facilities = new QuadTree<>(bounds.minX, bounds.minY, bounds.maxX, bounds.maxY);
        for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
            add(facility);
        }
    }

    public synchronized void add(ActivityFacility facility) {
        Coord coord = facility.getCoord();
        if (coord != null) {
            facilities.put(coord.getX(), coord.getY(), facility);
        }
    }

    public synchronized ActivityFacility randomWithin(Coord center,
                                  double radiusMeters,
                                  String activityType,
                                  Random random) {
        Collection<ActivityFacility> candidates = facilities.getDisk(
                center.getX(), center.getY(), Math.max(0.0, radiusMeters));
        ActivityFacility selected = null;
        int matchingCandidates = 0;
        for (ActivityFacility candidate : candidates) {
            if (!supports(candidate, activityType)) {
                continue;
            }
            matchingCandidates++;
            // Reservoir sampling avoids allocating a filtered candidate list.
            if (random.nextInt(matchingCandidates) == 0) {
                selected = candidate;
            }
        }
        return selected;
    }

    synchronized ActivityFacility nearest(Coord center) {
        return facilities.size() == 0 ? null : facilities.getClosest(center.getX(), center.getY());
    }

    public synchronized int size() {
        return facilities.size();
    }

    private boolean supports(ActivityFacility facility, String activityType) {
        return facility.getActivityOptions().isEmpty()
                || facility.getActivityOptions().containsKey(activityType);
    }

    private record Bounds(double minX, double minY, double maxX, double maxY) {
        static Bounds from(ActivityFacilities activityFacilities, Network network) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;

            for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
                Coord coord = facility.getCoord();
                if (coord != null) {
                    minX = Math.min(minX, coord.getX());
                    minY = Math.min(minY, coord.getY());
                    maxX = Math.max(maxX, coord.getX());
                    maxY = Math.max(maxY, coord.getY());
                }
            }
            for (var node : network.getNodes().values()) {
                Coord coord = node.getCoord();
                minX = Math.min(minX, coord.getX());
                minY = Math.min(minY, coord.getY());
                maxX = Math.max(maxX, coord.getX());
                maxY = Math.max(maxY, coord.getY());
            }

            if (!Double.isFinite(minX)) {
                minX = minY = -BOUNDS_PADDING_METERS;
                maxX = maxY = BOUNDS_PADDING_METERS;
            }
            return new Bounds(
                    minX - BOUNDS_PADDING_METERS,
                    minY - BOUNDS_PADDING_METERS,
                    maxX + BOUNDS_PADDING_METERS,
                    maxY + BOUNDS_PADDING_METERS
            );
        }
    }
}
