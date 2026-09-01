package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import com.google.inject.Provider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.network_calibration.demand_calibration.Tools;
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
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;
import org.matsim.utils.objectattributes.attributable.Attributable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

public final class CrossBorderCloneFactory {
    private static final Logger logger = LogManager.getLogger(CrossBorderCloneFactory.class);
    private static final Set<String> NETWORK_ROUTING_MODES = Set.of(TransportMode.car, TransportMode.truck);

    static final String CLONE_ID_SUFFIX = "_cbexpand";
    private final org.matsim.api.core.v01.population.Population population;
    private final org.matsim.api.core.v01.network.Network network;
    private final ActivityFacilities facilities;
    private final CrossBorderFacilityIndex facilitySpatialIndex;
    private final Vehicles vehicles;
    private final TripListConverter tripListConverter;
    private final Provider<TripRouter> tripRouterProvider;
    private final Random random;
    private final double relocationRadiusMeters;
    private final double homeRelocationRadiusMeters;
    private final int maximumTimeShiftSeconds;
    private final ThreadLocal<PlanRouter> planRouter;
    private final java.util.concurrent.atomic.AtomicInteger personCloneCounter = new java.util.concurrent.atomic.AtomicInteger();
    private final java.util.concurrent.atomic.AtomicInteger activityCloneCounter = new java.util.concurrent.atomic.AtomicInteger();

    public CrossBorderCloneFactory(Scenario scenario,
                            TripListConverter tripListConverter,
                            Provider<TripRouter> tripRouterProvider,
                            Random random,
                            double relocationRadiusMeters,
                            double homeRelocationRadiusMeters,
                            int maximumTimeShiftSeconds) {
        this.population = scenario.getPopulation();
        this.network = scenario.getNetwork();
        this.facilities = scenario.getActivityFacilities();
        this.facilitySpatialIndex = new CrossBorderFacilityIndex(facilities, network);
        this.vehicles = scenario.getVehicles();
        this.tripListConverter = tripListConverter;
        this.tripRouterProvider = tripRouterProvider;
        this.planRouter = ThreadLocal.withInitial(() -> new PlanRouter(facilities, tripRouterProvider.get()));
        this.random = random;
        if (!Double.isFinite(relocationRadiusMeters) || relocationRadiusMeters <= 0.0) {
            throw new IllegalArgumentException("relocationRadiusMeters must be finite and strictly positive");
        }
        this.relocationRadiusMeters = relocationRadiusMeters;
        if (!Double.isFinite(homeRelocationRadiusMeters) || homeRelocationRadiusMeters <= 0.0) {
            throw new IllegalArgumentException("homeRelocationRadiusMeters must be finite and strictly positive");
        }
        if (maximumTimeShiftSeconds < 0) {
            throw new IllegalArgumentException("maximumTimeShiftSeconds must be non-negative");
        }
        this.homeRelocationRadiusMeters = homeRelocationRadiusMeters;
        this.maximumTimeShiftSeconds = maximumTimeShiftSeconds;
    }

    /** Creates and routes one clone without adding it to the population yet. */
    public Person prepareClone(Id<Person> donorId) {
        Person donor;
        synchronized (population) {
            donor = population.getPersons().get(donorId);
        }
        if (donor == null || notCrossBorderPerson(donor)) {
            return null;
        }

        return createClone(donor, planRouter.get());
    }

    /** Commits a routed clone after the calibration acceptance test succeeds. */
    public void commitClone(Person clone) {
        synchronized (population) {
            population.addPerson(clone);
        }
    }

    /** Rolls back a routed clone that fails the calibration acceptance test. */
    public void discardClone(Person clone) {
        removeCloneVehicles(clone);
    }

    private Person createClone(Person donor, PlanRouter planRouter) {
        if (notCrossBorderPerson(donor)) {
            return null;
        }

        Plan donorPlan = donor.getSelectedPlan();
        if (donorPlan == null) {
            return null;
        }

        Id<Person> cloneId = buildCloneId(donor);
        Person clone = population.getFactory().createPerson(cloneId);

        copyPersonAttributes(donor, clone);
        clone.getAttributes().putAttribute(CrossBorderState.CLONED_ATTRIBUTE, true);

        registerCloneVehicles(donor, clone);
        if (!validateCloneVehicleIsolation(donor, clone)) {
            removeCloneVehicles(clone);
            return null;
        }

        Plan clonedPlan;
        try {
            clonedPlan = clonePlan(donorPlan, clone);
        } catch (RuntimeException exception) {
            logger.warn("Cross-border clone skipped for donor {}: {}",
                    donor.getId(), exception.getMessage());
            removeCloneVehicles(clone);
            return null;
        }
        if (clonedPlan == null) {
            removeCloneVehicles(clone);
            return null;
        }

        clone.addPlan(clonedPlan);
        clone.setSelectedPlan(clonedPlan);

        if (!reroutePlan(clone, clonedPlan, planRouter)) {
            removeCloneVehicles(clone);
            return null;
        }
        if (!fixedActivityLocationsArePreserved(donorPlan, clonedPlan)) {
            logger.warn("Cross-border clone skipped for donor {}: routing changed a border or connector activity location.",
                    donor.getId());
            removeCloneVehicles(clone);
            return null;
        }

        return clone;
    }

    private Id<Person> buildCloneId(Person donor) {
        String prefix = donor.getId().toString() + CLONE_ID_SUFFIX;
        Id<Person> cloneId;
        synchronized (population) {
            do {
                cloneId = Id.createPersonId(prefix + personCloneCounter.incrementAndGet());
            } while (population.getPersons().containsKey(cloneId));
        }
        return cloneId;
    }

    private synchronized void registerCloneVehicles(Person donor, Person clone) {
        Map<String, Id<Vehicle>> donorVehicleIds = VehicleUtils.getVehicleIds(donor);
        Set<String> modesToRegister = new LinkedHashSet<>(donorVehicleIds.keySet());
        Map<String, Id<Vehicle>> cloneVehicleIds = new HashMap<>();

        Plan donorPlan = donor.getSelectedPlan();
        if (donorPlan != null) {
            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(donorPlan)) {
                if (Tools.isCarOrTruck(trip)) {
                    modesToRegister.add(trip.getInitialMode());
                }
            }
        }

        for (String mode : modesToRegister) {
            Id<Vehicle> donorVehId = donorVehicleIds.get(mode);
            Vehicle donorVehicle = donorVehId != null ? vehicles.getVehicles().get(donorVehId) : null;
            VehicleType type = resolveVehicleType(mode, donorVehicle);

            Id<Vehicle> cloneVehId = VehicleUtils.createVehicleId(clone, mode);
            if (!vehicles.getVehicles().containsKey(cloneVehId)) {
                Vehicle cloneVehicle = vehicles.getFactory().createVehicle(cloneVehId, type);
                vehicles.addVehicle(cloneVehicle);
            }
            cloneVehicleIds.put(mode, cloneVehId);
        }

        if (!cloneVehicleIds.isEmpty()) {
            VehicleUtils.insertVehicleIdsIntoPersonAttributes(clone, cloneVehicleIds);
        }
    }

    private VehicleType resolveVehicleType(String mode, Vehicle donorVehicle) {
        if (donorVehicle != null && donorVehicle.getType() != null) {
            return donorVehicle.getType();
        }

        Id<VehicleType> modeTypeId = Id.create(mode, VehicleType.class);
        VehicleType modeType = vehicles.getVehicleTypes().get(modeTypeId);
        if (modeType != null) {
            return modeType;
        }

        if (!vehicles.getVehicleTypes().isEmpty()) {
            return vehicles.getVehicleTypes().values().iterator().next();
        }

        VehicleType fallbackType = vehicles.getFactory().createVehicleType(modeTypeId);
        vehicles.addVehicleType(fallbackType);
        return fallbackType;
    }

    private boolean validateCloneVehicleIsolation(Person donor, Person clone) {
        Map<String, Id<Vehicle>> donorVehicleIds = VehicleUtils.getVehicleIds(donor);
        Map<String, Id<Vehicle>> cloneVehicleIds = VehicleUtils.getVehicleIds(clone);

        for (Map.Entry<String, Id<Vehicle>> cloneEntry : cloneVehicleIds.entrySet()) {
            String mode = cloneEntry.getKey();
            Id<Vehicle> cloneVehicleId = cloneEntry.getValue();
            Id<Vehicle> donorVehicleId = donorVehicleIds.get(mode);

            if (cloneVehicleId != null && cloneVehicleId.equals(donorVehicleId)) {
                logger.warn("Cross-border clone skipped: clone {} and donor {} share {} vehicle {}.",
                        clone.getId(), donor.getId(), mode, cloneVehicleId);
                return false;
            }
        }

        return true;
    }

    private synchronized void removeCloneVehicles(Person clone) {
        for (Id<Vehicle> vehicleId : VehicleUtils.getVehicleIds(clone).values()) {
            if (vehicleId != null) {
                vehicles.removeVehicle(vehicleId);
            }
        }
    }

    private void copyPersonAttributes(Person donor, Person clone) {
        for (Map.Entry<String, Object> attr : donor.getAttributes().getAsMap().entrySet()) {
            if (shouldSkipAttribute(attr.getKey())) {
                continue;
            }
            clone.getAttributes().putAttribute(attr.getKey(), copyAttributeValue(attr.getValue()));
        }
    }

    private boolean shouldSkipAttribute(String key) {
        String normalized = key == null ? "" : key.toLowerCase(Locale.ROOT);
        return normalized.contains("vehicle");
    }

    private static Object copyAttributeValue(Object value) {
        if (value instanceof Map<?, ?> mapValue) {
            return new HashMap<>(mapValue);
        }
        if (value instanceof List<?> listValue) {
            return new ArrayList<>(listValue);
        }
        if (value instanceof Set<?> setValue) {
            return new LinkedHashSet<>(setValue);
        }
        return value;
    }

    private Plan clonePlan(Plan donorPlan, Person cloneOwner) {
        if (donorPlan == null) {
            return null;
        }

        Plan newPlan = population.getFactory().createPlan();
        newPlan.setPerson(cloneOwner);

        int requestedTimeShiftSeconds = maximumTimeShiftSeconds == 0
                ? 0
                : random.nextInt(2 * maximumTimeShiftSeconds + 1) - maximumTimeShiftSeconds;
        int timeShiftSeconds = constrainTimeShiftToNonNegativeTimes(
                donorPlan, requestedTimeShiftSeconds);
        for (PlanElement element : donorPlan.getPlanElements()) {
            if (element instanceof Activity activity) {
                newPlan.addActivity(cloneActivity(activity, timeShiftSeconds));
            } else if (element instanceof Leg leg) {
                newPlan.addLeg(cloneLeg(leg, timeShiftSeconds));
            }
        }

        synchronizeLegDeparturesWithActivities(newPlan);

        if (newPlan.getPlanElements().isEmpty()) {
            return null;
        }

        copyPlanMetadata(donorPlan, newPlan);
        return newPlan;
    }

    /**
     * Copies plan metadata without calling MATSim's primitive-valued convenience
     * getters. In particular, {@code Plan#getIterationCreated()} unboxes an
     * optional attribute and throws when plans loaded from input have no plan
     * inheritance metadata.
     */
    static void copyPlanMetadata(Plan source, Plan target) {
        target.setScore(source.getScore());
        target.setType(source.getType());
        copyAttributes(source, target);
    }

    private Leg cloneLeg(Leg donorLeg, int timeShiftSeconds) {
        Leg copy = population.getFactory().createLeg(donorLeg.getMode());
        copy.setRoutingMode(donorLeg.getRoutingMode() != null ? donorLeg.getRoutingMode() : donorLeg.getMode());
        donorLeg.getDepartureTime().ifDefined(
                departureTime -> copy.setDepartureTime(departureTime + timeShiftSeconds));
        donorLeg.getTravelTime().ifDefined(copy::setTravelTime);
        copyAttributes(donorLeg, copy);
        if (donorLeg.getRoute() != null) {
            copy.setRoute(donorLeg.getRoute().clone());
        }
        return copy;
    }

    private Activity cloneActivity(Activity original, int timeShiftSeconds) {
        Coord baseCoord = original.getCoord();
        if (baseCoord == null && original.getFacilityId() != null) {
            ActivityFacility facility = facilities.getFacilities().get(original.getFacilityId());
            if (facility != null) {
                baseCoord = facility.getCoord();
            }
        }
        if (baseCoord == null && original.getLinkId() != null) {
            Link link = network.getLinks().get(original.getLinkId());
            if (link != null) {
                baseCoord = link.getCoord();
            }
        }

        if (baseCoord == null) {
            throw new IllegalStateException("Cannot determine coordinates for activity " + original.getType());
        }

        Activity clone = population.getFactory().createActivityFromCoord(original.getType(), baseCoord);
        copyAttributes(original, clone);
        original.getStartTime().ifDefined(clone::setStartTime);
        original.getEndTime().ifDefined(clone::setEndTime);
        original.getMaximumDuration().ifDefined(clone::setMaximumDuration);
        shiftActivityTime(clone, timeShiftSeconds);
        // randomly shift the location of the activity
        if (!CrossBorderActivityRules.isFixedLocation(original)) {
            ActivityFacility newFacility = relocateActivity(baseCoord, original.getType());
            clone.setCoord(newFacility.getCoord());
            clone.setFacilityId(newFacility.getId());
            clone.setLinkId(linkForRelocatedActivity(newFacility, network));
        } else {
            clone.setCoord(baseCoord);
            clone.setFacilityId(original.getFacilityId());
            clone.setLinkId(original.getLinkId());
        }

        return clone;
    }

    /** Keeps the activity and its referenced facility attached to the same network link. */
    static Id<Link> linkForRelocatedActivity(ActivityFacility facility,
                                             org.matsim.api.core.v01.network.Network network) {
        if (facility.getLinkId() != null) {
            return facility.getLinkId();
        }

        Link nearestLink = NetworkUtils.getNearestLink(network, facility.getCoord());
        if (nearestLink == null) {
            throw new IllegalStateException("Cannot determine a link for relocated facility " + facility.getId());
        }
        FacilitiesUtils.setLinkID(facility, nearestLink.getId());
        return nearestLink.getId();
    }

    private synchronized ActivityFacility relocateActivity(Coord original, String activityType) {
        double radius = "home".equals(activityType) ? homeRelocationRadiusMeters : relocationRadiusMeters;

        ActivityFacility candidate = facilitySpatialIndex.randomWithin(
                original, radius, activityType, random);
        if (candidate != null) {
            return candidate;
        }
        Link randomLink = getRandomLinkWithinRadius(original, radius);
        if (randomLink != null) {
            ActivityFacility newFacility = facilities.getFactory().createActivityFacility(buildCloneActivityId(activityType),
                                                                                          randomLink.getCoord(),
                                                                                          randomLink.getId());
            newFacility.addActivityOption(facilities.getFactory().createActivityOption(activityType));
            facilities.addActivityFacility(newFacility);
            facilitySpatialIndex.add(newFacility);
            return newFacility;
        }
        return findNearestFacility(original);
    }

    private Id<ActivityFacility> buildCloneActivityId(String actType) {
        String prefix = "clone_" + actType + "_";
        Id<ActivityFacility> cloneId;
        do {
            cloneId = Id.create(prefix + activityCloneCounter.incrementAndGet(), ActivityFacility.class);
        } while (facilities.getFacilities().containsKey(cloneId));
        return cloneId;
    }

    private void shiftActivityTime(Activity activity, int timeShiftSeconds) {
        if (activity.getStartTime().isDefined()) {
            activity.setStartTime(activity.getStartTime().seconds() + timeShiftSeconds);
        }
        if (activity.getEndTime().isDefined()) {
            activity.setEndTime(activity.getEndTime().seconds() + timeShiftSeconds);
        }
    }

    /** Keeps a uniform plan-wide shift while ensuring no defined time becomes negative. */
    static int constrainTimeShiftToNonNegativeTimes(Plan plan, int requestedShiftSeconds) {
        double earliestDefinedTime = Double.POSITIVE_INFINITY;
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity activity) {
                if (activity.getStartTime().isDefined()) {
                    earliestDefinedTime = Math.min(
                            earliestDefinedTime, activity.getStartTime().seconds());
                }
                if (activity.getEndTime().isDefined()) {
                    earliestDefinedTime = Math.min(
                            earliestDefinedTime, activity.getEndTime().seconds());
                }
            } else if (element instanceof Leg leg && leg.getDepartureTime().isDefined()) {
                earliestDefinedTime = Math.min(
                        earliestDefinedTime, leg.getDepartureTime().seconds());
            }
        }

        if (!Double.isFinite(earliestDefinedTime)) {
            return requestedShiftSeconds;
        }
        return Math.max(requestedShiftSeconds, (int) Math.ceil(-earliestDefinedTime));
    }

    /**
     * MATSim executes a leg when its preceding activity ends. Keep the explicit
     * leg departure metadata consistent as well, including legs not rerouted by
     * {@link PlanRouter}.
     */
    static void synchronizeLegDeparturesWithActivities(Plan plan) {
        Activity precedingActivity = null;
        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity activity) {
                precedingActivity = activity;
            } else if (element instanceof Leg leg
                    && precedingActivity != null
                    && precedingActivity.getEndTime().isDefined()) {
                leg.setDepartureTime(precedingActivity.getEndTime().seconds());
            }
        }
    }

    private Link getRandomLinkWithinRadius(Coord center, double radiusMeters) {
        if (network.getLinks().isEmpty()) {
            return null;
        }
        double angle = random.nextDouble() * 2.0 * Math.PI;
        double distance = Math.sqrt(random.nextDouble()) * radiusMeters;
        double randomX = Math.cos(angle) * distance;
        double randomY = Math.sin(angle) * distance;
        Coord newCoords = new Coord(center.getX() + randomX, center.getY() + randomY);
        return NetworkUtils.getNearestLink(network, newCoords);
    }

    private ActivityFacility findNearestFacility(Coord center) {
        ActivityFacility nearest = facilitySpatialIndex.nearest(center);

        if (nearest == null) {
            throw new IllegalStateException("No facilities available for activity relocation.");
        }

        return nearest;
    }

    private boolean reroutePlan(Person clone, Plan plan, PlanRouter planRouter) {
        try {
            planRouter.run(plan, true, NETWORK_ROUTING_MODES);
            synchronizeLegDeparturesWithActivities(plan);
        } catch (Exception e) {
            logger.warn("Plan routing failed for clone {}: {}", clone.getId(), e.getMessage());
            return false;
        }

        for (PlanElement element : plan.getPlanElements()) {
            if (!(element instanceof Leg leg) || !(leg.getRoute() instanceof NetworkRoute route)) {
                continue;
            }

            String routingMode = leg.getRoutingMode() != null ? leg.getRoutingMode() : leg.getMode();
            Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(clone, routingMode);
            if (vehicleId == null) {
                logger.warn("Clone {} has no vehicle for routed mode {}.", clone.getId(), routingMode);
                return false;
            }
            route.setVehicleId(vehicleId);
        }

        return true;
    }

    private boolean notCrossBorderPerson(Person person) {
        return !Tools.isCrossBorderPerson(person);
    }

    private boolean fixedActivityLocationsArePreserved(Plan donorPlan, Plan clonedPlan) {
        List<Activity> donorActivities = donorPlan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .map(Activity.class::cast)
                .toList();
        List<Activity> clonedActivities = clonedPlan.getPlanElements().stream()
                .filter(Activity.class::isInstance)
                .map(Activity.class::cast)
                .toList();
        if (donorActivities.size() != clonedActivities.size()) {
            return false;
        }

        for (int index = 0; index < donorActivities.size(); index++) {
            Activity donor = donorActivities.get(index);
            if (!CrossBorderActivityRules.isFixedLocation(donor)) {
                continue;
            }

            Activity clone = clonedActivities.get(index);
            if (!sameCoord(resolveActivityCoord(donor), clone.getCoord())
                    || !Objects.equals(donor.getFacilityId(), clone.getFacilityId())
                    || !Objects.equals(donor.getLinkId(), clone.getLinkId())) {
                return false;
            }
        }
        return true;
    }

    private Coord resolveActivityCoord(Activity activity) {
        if (activity.getCoord() != null) {
            return activity.getCoord();
        }
        if (activity.getFacilityId() != null) {
            ActivityFacility facility = facilities.getFacilities().get(activity.getFacilityId());
            if (facility != null) {
                return facility.getCoord();
            }
        }
        if (activity.getLinkId() != null) {
            Link link = network.getLinks().get(activity.getLinkId());
            if (link != null) {
                return link.getCoord();
            }
        }
        return null;
    }

    private boolean sameCoord(Coord first, Coord second) {
        if (first == null || second == null) {
            return first == second;
        }
        return Math.abs(first.getX() - second.getX()) <= 1.0e-6
                && Math.abs(first.getY() - second.getY()) <= 1.0e-6;
    }

    private static void copyAttributes(Attributable source, Attributable target) {
        source.getAttributes().getAsMap().forEach((key, value) ->
                target.getAttributes().putAttribute(key, copyAttributeValue(value)));
    }
}
