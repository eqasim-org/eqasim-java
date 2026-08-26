package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.background;

import com.google.inject.Provider;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
import org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder.CrossBorderActivityRules;
import org.eqasim.core.scenario.routing.PlanRouter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates isolated, routed relocation proposals for background traffic.
 * Freight moves one trip endpoint; cross-border traffic moves the central
 * non-anchor activity shared by its arriving and departing trips.
 */
public final class BackgroundPlanRelocator {
    private final Scenario scenario;
    private final FacilityIndex facilities;
    private final AnchorStore anchors;
    private final ThreadLocal<PlanRouter> planRouter;
    private final double destinationSelectionProbability;

    public BackgroundPlanRelocator(Scenario scenario,
                                   Provider<TripRouter> tripRouterProvider,
                                   double radiusFactor,
                                   double minimumRadius,
                                   double maximumRadius,
                                   double destinationSelectionProbability) {
        this.scenario = scenario;
        this.facilities = new FacilityIndex(scenario.getActivityFacilities());
        this.destinationSelectionProbability = destinationSelectionProbability;
        this.anchors = new AnchorStore(scenario, radiusFactor, minimumRadius, maximumRadius);
        this.planRouter = ThreadLocal.withInitial(() ->
                new PlanRouter(scenario.getActivityFacilities(), tripRouterProvider.get()));
    }

    public Plan propose(Person owner, Random random) {
        Plan source = owner.getSelectedPlan();
        if (source == null) return null;
        List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(source);

        if (Tools.isFreightPerson(owner) && trips.size() == 1) {
            return proposeFreight(owner, source, random);
        }
        if (Tools.isCrossBorderPerson(owner) && trips.size() > 1) {
            return proposeCrossBorder(owner, source, random);
        }
        return null;
    }

    private Plan proposeFreight(Person owner, Plan source, Random random) {
        FreightAnchor anchor = anchors.freight(owner);
        if (anchor == null) return null;

        boolean relocateOrigin = random.nextDouble() > destinationSelectionProbability;
        Plan candidate = copyPlan(source, owner);
        List<TripStructureUtils.Trip> candidateTrips = TripStructureUtils.getTrips(candidate);
        if (candidateTrips.size() != 1) return null;

        Activity activity = relocateOrigin
                ? candidateTrips.getFirst().getOriginActivity()
                : candidateTrips.getFirst().getDestinationActivity();
        if (!relocate(activity, anchor.endpoint(relocateOrigin), anchor.radius(), null, random)) {
            return null;
        }
        return routeCandidate(owner, candidate, activity);
    }

    private Plan proposeCrossBorder(Person owner, Plan source, Random random) {
        CrossBorderAnchor anchor = anchors.crossBorder(owner);
        if (anchor == null) return null;

        Plan candidate = copyPlan(source, owner);
        List<TripStructureUtils.Trip> candidateTrips = TripStructureUtils.getTrips(candidate);
        if (candidateTrips.size() != anchor.tripCount()) return null;

        Activity activity = sharedActivity(candidateTrips, anchor.activityIndex());
        if (activity == null || CrossBorderActivityRules.isFixedLocation(activity)) return null;
        if (!relocate(activity, anchor.coordinate(), anchor.radius(), activity.getType(), random)) {
            return null;
        }
        return routeCandidate(owner, candidate, activity);
    }

    private boolean relocate(Activity activity,
                             Coord center,
                             double radius,
                             String requiredActivityType,
                             Random random) {
        ActivityFacility current = activity.getFacilityId() == null ? null
                : scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
        ActivityFacility replacement = facilities.randomWithin(
                center, radius, current, requiredActivityType, random);
        if (replacement == null) return false;

        activity.setCoord(replacement.getCoord());
        activity.setFacilityId(replacement.getId());
        if (replacement.getLinkId() != null) {
            activity.setLinkId(replacement.getLinkId());
        } else {
            Link nearest = NetworkUtils.getNearestLink(scenario.getNetwork(), replacement.getCoord());
            if (nearest != null && nearest.getAllowedModes().contains(TransportMode.car)){
                activity.setLinkId(nearest.getId());
            } else{
                activity.setLinkId(null);
            }
        }
        return true;
    }

    /** Reroutes only the trip or two trips incident to the moved activity. */
    private Plan routeCandidate(Person owner, Plan candidate, Activity movedActivity) {
        try {
            planRouter.get().run(candidate, true, Set.of(), trip ->
                    trip.getOriginActivity() == movedActivity
                            || trip.getDestinationActivity() == movedActivity);
            assignVehicles(owner, candidate);
            return candidate;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private Plan copyPlan(Plan source, Person owner) {
        Plan candidate = scenario.getPopulation().getFactory().createPlan();
        candidate.setPerson(owner);
        PopulationUtils.copyFromTo(source, candidate, true);
        return candidate;
    }

    private void assignVehicles(Person owner, Plan plan) {
        for (PlanElement element : plan.getPlanElements()) {
            if (!(element instanceof Leg leg) || !(leg.getRoute() instanceof NetworkRoute route)) {
                continue;
            }
            String mode = leg.getRoutingMode() == null ? leg.getMode() : leg.getRoutingMode();
            Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(owner, mode);
            if (vehicleId != null) route.setVehicleId(vehicleId);
        }
    }

    static int centralMovableActivityIndex(List<TripStructureUtils.Trip> trips) {
        int selected = -1;
        double selectedDistance = Double.POSITIVE_INFINITY;
        double center = trips.size() / 2.0 - 1e-3; // if right in middle, prefer the first one
        for (int activityIndex = 1; activityIndex < trips.size(); activityIndex++) {
            Activity activity = sharedActivity(trips, activityIndex);
            if (activity == null || CrossBorderActivityRules.isFixedLocation(activity)) continue;
            double distance = Math.abs(activityIndex - center);
            if (distance < selectedDistance) {
                selected = activityIndex;
                selectedDistance = distance;
            }
        }
        return selected;
    }

    static Activity sharedActivity(List<TripStructureUtils.Trip> trips, int activityIndex) {
        if (activityIndex <= 0 || activityIndex >= trips.size()) return null;
        Activity arrivingDestination = trips.get(activityIndex - 1).getDestinationActivity();
        Activity departingOrigin = trips.get(activityIndex).getOriginActivity();
        return arrivingDestination == departingOrigin ? arrivingDestination : null;
    }

    static double searchRadius(double crowFlyDistance,
                               double radiusFactor,
                               double minimumRadius,
                               double maximumRadius) {
        return Math.max(minimumRadius, Math.min(maximumRadius, radiusFactor * crowFlyDistance));
    }

    /** Read-only spatial index shared safely by all proposal workers. */
    private static final class FacilityIndex {
        private final QuadTree<ActivityFacility> facilities;

        private FacilityIndex(ActivityFacilities activityFacilities) {
            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
                Coord coord = facility.getCoord();
                if (coord == null) continue;
                minX = Math.min(minX, coord.getX());
                minY = Math.min(minY, coord.getY());
                maxX = Math.max(maxX, coord.getX());
                maxY = Math.max(maxY, coord.getY());
            }
            if (!Double.isFinite(minX)) {
                minX = minY = -1.0;
                maxX = maxY = 1.0;
            }

            facilities = new QuadTree<>(minX - 1.0, minY - 1.0, maxX + 1.0, maxY + 1.0);
            for (ActivityFacility facility : activityFacilities.getFacilities().values()) {
                if (facility.getCoord() != null) {
                    facilities.put(facility.getCoord().getX(), facility.getCoord().getY(), facility);
                }
            }
        }

        /** Reservoir sampling avoids allocating a filtered candidate list. */
        private ActivityFacility randomWithin(Coord center,
                                              double radius,
                                              ActivityFacility excluded,
                                              String requiredActivityType,
                                              Random random) {
            Collection<ActivityFacility> nearby = facilities.getDisk(center.getX(), center.getY(), radius);
            ActivityFacility selected = null;
            int seen = 0;
            double radiusSquared = radius * radius;

            for (ActivityFacility facility : nearby) {
                if (facility == excluded || facility.getCoord() == null) continue;
                if (requiredActivityType != null
                        && !facility.getActivityOptions().isEmpty()
                        && !facility.getActivityOptions().containsKey(requiredActivityType)) continue;
                double dx = facility.getCoord().getX() - center.getX();
                double dy = facility.getCoord().getY() - center.getY();
                if (dx * dx + dy * dy > radiusSquared) continue;
                seen++;
                if (random.nextInt(seen) == 0) selected = facility;
            }
            return selected;
        }
    }

    /** Captures immutable startup anchors so repeated corrections cannot drift. */
    static final class AnchorStore {
        private final Scenario scenario;
        private final double radiusFactor;
        private final double minimumRadius;
        private final double maximumRadius;
        private final Map<Id<Person>, FreightAnchor> freightAnchors = new ConcurrentHashMap<>();
        private final Map<Id<Person>, CrossBorderAnchor> crossBorderAnchors = new ConcurrentHashMap<>();

        AnchorStore(Scenario scenario,
                    double radiusFactor,
                    double minimumRadius,
                    double maximumRadius) {
            this.scenario = scenario;
            this.radiusFactor = radiusFactor;
            this.minimumRadius = minimumRadius;
            this.maximumRadius = maximumRadius;
            scenario.getPopulation().getPersons().values().forEach(this::capture);
        }

        FreightAnchor freight(Person person) {
            return freightAnchors.computeIfAbsent(person.getId(), ignored -> createFreight(person.getSelectedPlan()));
        }

        CrossBorderAnchor crossBorder(Person person) {
            return crossBorderAnchors.computeIfAbsent(person.getId(), ignored -> createCrossBorder(person.getSelectedPlan()));
        }

        private void capture(Person person) {
            if (Tools.isFreightPerson(person)) {
                FreightAnchor anchor = createFreight(person.getSelectedPlan());
                if (anchor != null) freightAnchors.putIfAbsent(person.getId(), anchor);
            }
            if (Tools.isCrossBorderPerson(person)) {
                CrossBorderAnchor anchor = createCrossBorder(person.getSelectedPlan());
                if (anchor != null) crossBorderAnchors.putIfAbsent(person.getId(), anchor);
            }
        }

        private FreightAnchor createFreight(Plan plan) {
            if (plan == null) return null;
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
            if (trips.size() != 1) return null;
            Coord origin = coordinate(trips.getFirst().getOriginActivity());
            Coord destination = coordinate(trips.getFirst().getDestinationActivity());
            if (origin == null || destination == null) return null;
            double distance = distance(origin, destination);
            return new FreightAnchor(origin, destination,
                    searchRadius(distance, radiusFactor, minimumRadius, maximumRadius));
        }

        private CrossBorderAnchor createCrossBorder(Plan plan) {
            if (plan == null) return null;
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(plan);
            int activityIndex = centralMovableActivityIndex(trips);
            Activity activity = sharedActivity(trips, activityIndex);
            if (activity == null) return null;

            Coord previous = coordinate(trips.get(activityIndex - 1).getOriginActivity());
            Coord center = coordinate(activity);
            Coord next = coordinate(trips.get(activityIndex).getDestinationActivity());
            if (previous == null || center == null || next == null) return null;
            double localTripDistance = Math.max(distance(previous, center), distance(center, next));
            return new CrossBorderAnchor(activityIndex, trips.size(), center,
                    searchRadius(localTripDistance, radiusFactor, minimumRadius, maximumRadius));
        }

        private Coord coordinate(Activity activity) {
            if (activity.getCoord() != null) return activity.getCoord();
            if (activity.getFacilityId() != null) {
                ActivityFacility facility = scenario.getActivityFacilities().getFacilities().get(activity.getFacilityId());
                if (facility != null) return facility.getCoord();
            }
            if (activity.getLinkId() != null) {
                Link link = scenario.getNetwork().getLinks().get(activity.getLinkId());
                if (link != null) return link.getCoord();
            }
            return null;
        }

        private static double distance(Coord first, Coord second) {
            return Math.hypot(first.getX() - second.getX(), first.getY() - second.getY());
        }
    }

    record FreightAnchor(Coord origin, Coord destination, double radius) {
        FreightAnchor {
            origin = copy(origin);
            destination = copy(destination);
        }

        Coord endpoint(boolean originEndpoint) {
            return originEndpoint ? origin : destination;
        }
    }

    record CrossBorderAnchor(int activityIndex, int tripCount, Coord coordinate, double radius) {
        CrossBorderAnchor {
            coordinate = copy(coordinate);
        }
    }

    private static Coord copy(Coord coordinate) {
        if (coordinate == null) throw new IllegalArgumentException("Relocation anchor coordinate is required");
        return new Coord(coordinate.getX(), coordinate.getY());
    }
}
