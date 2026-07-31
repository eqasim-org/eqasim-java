package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations;

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
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class CrossBorderCloneFactory {
    private static final Logger logger = LogManager.getLogger(CrossBorderCloneFactory.class);
    private static final Set<String> NETWORK_ROUTING_MODES = Set.of(TransportMode.car, TransportMode.truck);

    static final String CLONE_ID_SUFFIX = "_cbexpand";
    static final List<String> ANCHOR_ACTIVITY_TYPE = List.of("outside", "border");
    static final List<String> ACTIVITY_TYPES = List.of("home","other","leisure","shop","work","education");

    private final org.matsim.api.core.v01.population.Population population;
    private final org.matsim.api.core.v01.network.Network network;
    private final ActivityFacilities facilities;
    private final Vehicles vehicles;
    private final TripListConverter tripListConverter;
    private final Provider<TripRouter> tripRouterProvider;
    private final Random random;
    private final double relocationRadiusMeters;
    private int cloneCounter = 0;
    private int clonedActivities = 0;

    CrossBorderCloneFactory(Scenario scenario,
                            TripListConverter tripListConverter,
                            Provider<TripRouter> tripRouterProvider,
                            Random random,
                            double relocationRadiusMeters) {
        this.population = scenario.getPopulation();
        this.network = scenario.getNetwork();
        this.facilities = scenario.getActivityFacilities();
        this.vehicles = scenario.getVehicles();
        this.tripListConverter = tripListConverter;
        this.tripRouterProvider = tripRouterProvider;
        this.random = random;
        this.relocationRadiusMeters = relocationRadiusMeters;
    }

    int applyCloning(List<Id<Person>> orderedDonorIds, int maxClones) {
        if (maxClones <= 0 || orderedDonorIds.isEmpty()) {
            return 0;
        }

        PlanRouter planRouter = new PlanRouter(facilities, tripRouterProvider.get());
        int cloned = 0;

        Set<Id<Person>> alreadyClonedDonors = new LinkedHashSet<>();
        for (Id<Person> donorId : orderedDonorIds) {
            if (cloned >= maxClones) {
                break;
            }

            if (!alreadyClonedDonors.add(donorId)) {
                continue;
            }

            Person donor = population.getPersons().get(donorId);
            if (donor == null) {
                continue;
            }
            if (notCrossBorderPerson(donor)) {
                continue;
            }

            Person clone = createClone(donor, planRouter);
            if (clone != null) {
                population.addPerson(clone);
                cloned++;
            }
        }

        return cloned;
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
        clone.getAttributes().putAttribute(State.CLONED_ATTRIBUTE, true);

        registerCloneVehicles(donor, clone);
        if (!validateCloneVehicleIsolation(donor, clone)) {
            return null;
        }

        Plan clonedPlan = clonePlan(donorPlan, clone);
        if (clonedPlan == null) {
            return null;
        }

        clone.addPlan(clonedPlan);
        clone.setSelectedPlan(clonedPlan);

        if (!reroutePlan(clone, clonedPlan, planRouter)) {
            return null;
        }

        return clone;
    }

    private Id<Person> buildCloneId(Person donor) {
        String prefix = donor.getId().toString() + CLONE_ID_SUFFIX;
        Id<Person> cloneId;
        do {
            cloneId = Id.createPersonId(prefix + (++cloneCounter));
        } while (population.getPersons().containsKey(cloneId));
        return cloneId;
    }

    private void registerCloneVehicles(Person donor, Person clone) {
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

    private Object copyAttributeValue(Object value) {
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

        for (PlanElement element : donorPlan.getPlanElements()) {
            if (element instanceof Activity activity) {
                newPlan.addActivity(cloneActivity(activity));
            } else if (element instanceof Leg leg) {
                newPlan.addLeg(cloneLeg(leg));
            }
        }

        if (newPlan.getPlanElements().isEmpty()) {
            return null;
        }

        newPlan.setScore(donorPlan.getScore());
        return newPlan;
    }

    private Leg cloneLeg(Leg donorLeg) {
        Leg copy = population.getFactory().createLeg(donorLeg.getMode());
        copy.setRoutingMode(donorLeg.getRoutingMode() != null ? donorLeg.getRoutingMode() : donorLeg.getMode());
        donorLeg.getDepartureTime().ifDefined(copy::setDepartureTime);
        donorLeg.getTravelTime().ifDefined(copy::setTravelTime);
        copy.setRoute(donorLeg.getRoute());
        return copy;
    }

    private Activity cloneActivity(Activity original) {
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
        original.getStartTime().ifDefined(clone::setStartTime);
        original.getEndTime().ifDefined(clone::setEndTime);
        original.getMaximumDuration().ifDefined(clone::setMaximumDuration);
        // randomly shift time
        shiftActivityTime(clone);
        // randomly shift the location of the activity
        if (!isFixedLocationActivity(original)) {
            ActivityFacility newFacility = relocateActivity(baseCoord, original.getType());
            clone.setCoord(newFacility.getCoord());
            clone.setFacilityId(newFacility.getId());

            Link nearestLink = NetworkUtils.getNearestLink(network, newFacility.getCoord());
            if (nearestLink != null) {
                clone.setLinkId(nearestLink.getId());
            } else if (newFacility.getLinkId() != null) {
                clone.setLinkId(newFacility.getLinkId());
            }
        } else {
            clone.setCoord(baseCoord);
            clone.setFacilityId(original.getFacilityId());
            clone.setLinkId(original.getLinkId());
        }

        return clone;
    }

    private ActivityFacility relocateActivity(Coord original, String activityType) {
        double radius = activityType.equals("home")? 500.0:relocationRadiusMeters;

        List<ActivityFacility> candidates = findFacilitiesWithinRadius(original, radius);
        if (!candidates.isEmpty()) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        Link randomLink = getRandomLinkWithinRadius(original, radius);
        if (randomLink != null) {
            Activity newActivity = population.getFactory().createActivityFromLinkId(activityType, randomLink.getId());
            ActivityFacility newFacility = facilities.getFactory().createActivityFacility(buildCloneActivityId(activityType),
                                                                                          newActivity.getCoord(),
                                                                                          randomLink.getId());
            facilities.addActivityFacility(newFacility);
            return newFacility;
        }
        return findNearestFacility(original);
    }

    private Id<ActivityFacility> buildCloneActivityId(String actType) {
        String prefix = "clone_" + actType + "_";
        Id<ActivityFacility> cloneId;
        do {
            cloneId = Id.create(prefix + (++cloneCounter), ActivityFacility.class);
        } while (facilities.getFacilities().containsKey(cloneId));
        return cloneId;
    }

    private void shiftActivityTime(Activity activity){
        int randomTimeShift = (random.nextInt(0, 1200) - 600);
        if (activity.getStartTime().isDefined()) {
            activity.setStartTime(Math.max(0.0, activity.getStartTime().seconds() + randomTimeShift));
        }
        if (activity.getEndTime().isDefined()) {
            activity.setEndTime(Math.max(0.0, activity.getEndTime().seconds() + randomTimeShift));
        }
    }

    private List<ActivityFacility> findFacilitiesWithinRadius(Coord center, double radiusMeters) {
        List<ActivityFacility> result = new ArrayList<>();
        double radiusSq = radiusMeters * radiusMeters;

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            Coord fc = facility.getCoord();
            double dx = fc.getX() - center.getX();
            double dy = fc.getY() - center.getY();
            if (dx * dx + dy * dy <= radiusSq) {
                result.add(facility);
            }
        }

        return result;
    }

    private Link getRandomLinkWithinRadius(Coord center, double radiusMeters) {
        double randomX = random.nextDouble() * radiusMeters - radiusMeters/2;
        double randomY = random.nextDouble() * radiusMeters - radiusMeters/2;
        Coord newCoords = new Coord(center.getX() + randomX, center.getY() + randomY);
        return NetworkUtils.getNearestLink(network, newCoords);
    }

    private ActivityFacility findNearestFacility(Coord center) {
        ActivityFacility nearest = null;
        double minDistSq = Double.POSITIVE_INFINITY;

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            Coord fc = facility.getCoord();
            double dx = fc.getX() - center.getX();
            double dy = fc.getY() - center.getY();
            double distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = facility;
            }
        }

        if (nearest == null) {
            throw new IllegalStateException("No facilities available for activity relocation.");
        }

        return nearest;
    }

    private boolean reroutePlan(Person clone, Plan plan, PlanRouter planRouter) {
        try {
            planRouter.run(plan, true, NETWORK_ROUTING_MODES);
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

    private boolean isAnchorActivity(Activity activity) {
        return ANCHOR_ACTIVITY_TYPE.contains(activity.getType());
    }

    private boolean notCrossBorderPerson(Person person) {
        return !Tools.isCrossBorderPerson(person);
    }

    private boolean isFixedLocationActivity(Activity activity) {
        return isAnchorActivity(activity) || isOneSecondConnector(activity);
    }

    private boolean isOneSecondConnector(Activity activity) {
        if (activity.getMaximumDuration().isDefined()) {
            return isOneSecond(activity.getMaximumDuration().seconds());
        }

        if (activity.getStartTime().isDefined() && activity.getEndTime().isDefined()) {
            double duration = activity.getEndTime().seconds() - activity.getStartTime().seconds();
            return isOneSecond(duration);
        }

        return false;
    }

    private boolean isOneSecond(double durationSeconds) {
        return Math.abs(durationSeconds - 1.0) <= 1.0e-6;
    }
}