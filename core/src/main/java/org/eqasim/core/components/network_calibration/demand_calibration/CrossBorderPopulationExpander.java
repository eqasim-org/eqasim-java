package org.eqasim.core.components.network_calibration.demand_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.network_calibration.NetworkCalibrationConfigGroup;
import org.eqasim.core.components.network_calibration.Processors.CountsProcessor;
import org.eqasim.core.components.network_calibration.Processors.FlowProcessor;
import org.eqasim.core.scenario.routing.PlanRouter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.replanning.TripListConverter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.TripRouter;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import com.google.inject.Provider;
import java.util.*;

public class CrossBorderPopulationExpander implements IterationEndsListener {

    private static final Logger logger = LogManager.getLogger(CrossBorderPopulationExpander.class);

    private static final boolean DEBUG = false;

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Minimum share of traversals that must be cross-border for a link to be eligible. */
    public static double CROSSBORDER_SHARE_THRESHOLD = DEBUG ? 0.0 : 0.50;

    /** Minimum relative under-estimation ( (counts-flow)/counts ) to trigger expansion. */
    public static double FLOW_UNDER_ESTIMATION_THRESHOLD = 0.15;

    /** Minimum traversals on a link before we trust its statistics. */
    public static int MIN_TRAVERSALS_PER_LINK = 20;

    // ── Expansion control ─────────────────────────────────────────────────────

    /**
     * Damping factor in [0,1]: we only clone a fraction of what is "needed"
     * each iteration to avoid overshooting. 0.7 means 70% of the gap.
     */
    public static double INITIAL_EXPANSION_DAMPING_FACTOR = 0.7;

    /** Maximum probability that any individual donor is selected for cloning. */
    public static double MAX_CLONE_PROBABILITY = 0.60;

    /** Minimum probability used as a floor once a donor is eligible. */
    public static double MIN_CLONE_PROBABILITY = 1.0e-3;

    // ── Activity relocation ───────────────────────────────────────────────────

    /** Radius (metres) within which a cloned agent's activity locations are perturbed. */
    public static double RELOCATION_RADIUS_METERS = 10_000.0;

    /** Activity type that is treated as the fixed anchor and is NOT relocated. */
    public static String ANCHOR_ACTIVITY_TYPE = "outside";

    // ── Scheduling ────────────────────────────────────────────────────────────

    /** Warm-up: no expansion before this iteration. */
    public static int WARM_UP_ITERATIONS = 30;

    /**
     * Offset from the warm-up start to stagger with SubPopulationReducer.
     * SubPopulationReducer: WARM_UP=30, INTERVAL=5 → fires at 30,35,40,…
     * This class with OFFSET=3                      → fires at 33,38,43,…
     */
    public static int SCHEDULE_OFFSET = 3;

    /** Iterations between expansion steps. Should match SubPopulationReducer's interval. */
    public static int ITERATION_INTERVAL = 5;

    // ── Cloned person ID ──────────────────────────────────────────────────────
    public static String CLONE_ID_SUFFIX = "_cbexpand";
    public static String CLONED_ATTRIBUTE = "cloned";

    // ─────────────────────────────────────────────────────────────────────────

    private final Population population;
    private final Network network;
    private final ActivityFacilities facilities;
    private final Vehicles vehicles;
    private final TripListConverter tripListConverter;
    private final CountsProcessor countsProcessor;
    private final FlowProcessor flowProcessor;
    private final Provider<TripRouter> tripRouterProvider;
    private final SubPopulationReducer subPopulationReducer;
    private final double sampleSize;
    private final boolean activate;
    private final boolean calibrate;
    private final Random random;
    private int cloneCounter = 0;
    private int numExpansions;
    private static final Set<String> NETWORK_ROUTING_MODES = Set.of(TransportMode.car, TransportMode.truck);


    public CrossBorderPopulationExpander(Scenario scenario,
                                         TripListConverter tripListConverter,
                                         Provider<CountsProcessor> countsProcessorProvider,
                                         Provider<FlowProcessor> flowProcessorProvider,
                                         Provider<TripRouter> tripRouterProvider,
                                         Provider<SubPopulationReducer> subPopulationReducerProvider,
                                         EqasimConfigGroup eqasimConfig,
                                         NetworkCalibrationConfigGroup calConfig) {
        this.population = scenario.getPopulation();
        this.network = scenario.getNetwork();
        this.facilities = scenario.getActivityFacilities();
        this.vehicles = scenario.getVehicles();
        this.tripListConverter = tripListConverter;
        this.tripRouterProvider = tripRouterProvider;
        this.sampleSize = eqasimConfig.getSampleSize();
        this.activate = calConfig.getAllObjectives().contains("subpopulations");
        this.calibrate = this.activate && calConfig.isCalibrationEnabled();
        this.random = MatsimRandom.getLocalInstance();
        this.numExpansions = 0;

        this.subPopulationReducer = calibrate ? subPopulationReducerProvider.get():null;
        this.countsProcessor = calibrate ? countsProcessorProvider.get():null;
        this.flowProcessor = calibrate ? flowProcessorProvider.get():null;
    }

    // =========================================================================
    //  MATSim listener entry point
    // =========================================================================

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!calibrate) {
            return;
        }
        int iteration = event.getIteration();

        if (DEBUG) {
            expandPopulation(iteration);
            return;
        }

        if (iteration < WARM_UP_ITERATIONS + SCHEDULE_OFFSET) {
            return;
        }
        if ((iteration - WARM_UP_ITERATIONS - SCHEDULE_OFFSET) % ITERATION_INTERVAL != 0) {
            return;
        }
        expandPopulation(iteration);
    }

    // =========================================================================
    //  Top-level orchestration
    // =========================================================================

    public void expandPopulation(int iteration) {
        // This will be incremented anyway, just to keep track of how many times this method is called
        numExpansions++;
        // Step 1 – build link-level under-estimation signals
        LinkStats linkStats = collectLinkStats();
        Map<Id<Link>, LinkSignal> signals = identifyUnderEstimatedLinks(linkStats);

        if (signals.isEmpty()) {
            logger.info("CrossBorderPopulationExpander [iter {}]: no eligible under-estimated links.", iteration);
            return;
        }

        // Step 2 – translate link signals to trip-level (deduplicating correlated stations),
        //          then to person-level clone probabilities
        Map<Id<Person>, Double> donorProbabilities = buildDonorProbabilities(signals);

        if (donorProbabilities.isEmpty()) {
            logger.info("CrossBorderPopulationExpander [iter {}]: {} eligible links but no CB donors found.",
                    iteration, signals.size());
            return;
        }

        int restored = restoreRemovedCrossBorderPersons(signals, donorProbabilities);

        if (restored > 0) {
            donorProbabilities = buildDonorProbabilities(signals);
            if (donorProbabilities.isEmpty()) {
            logger.info("CrossBorderPopulationExpander [iter {}]: restored {} reduced CB persons; no additional cloning needed.",
                iteration, restored);
            return;
            }
        }

        int initialNeed = estimateExpansionNeed(donorProbabilities);
        int remainingNeed = Math.max(0, initialNeed - restored);
        if (remainingNeed <= 0) {
            logger.info("CrossBorderPopulationExpander [iter {}]: restored {} reduced CB persons; cloning skipped.",
                iteration, restored);
            return;
        }

        // Step 3 – stochastically clone remaining donors, then re-route each clone
        PlanRouter planRouter = new PlanRouter(facilities, tripRouterProvider.get());
        int cloned = applyCloning(donorProbabilities, planRouter, remainingNeed);

        logger.info("CrossBorderPopulationExpander [iter {}]: {} eligible links, {} donor candidates, {} reduced CB restored, {} agents cloned.",
            iteration, signals.size(), donorProbabilities.size(), restored, cloned);
    }

    // =========================================================================
    //  Step 1 – collect link-level traversal counts
    // =========================================================================

    /**
     * Scans all selected plans and counts, per counted link:
     *   - total traversals (all agents, car/truck only)
     *   - cross-border traversals
     */
    private LinkStats collectLinkStats() {
        Map<Id<Link>, Integer> allTraversals = new HashMap<>();
        Map<Id<Link>, Integer> cbTraversals  = new HashMap<>();

        for (Person person : population.getPersons().values()) {
            Plan plan = person.getSelectedPlan();
            if (plan == null) continue;

            boolean isCB = isCrossBorderPerson(person);

            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
                if (!isCarOrTruck(trip)) continue;

                for (Id<Link> linkId : extractRouteLinkIds(trip)) {
                    if (!countsProcessor.contains(linkId)) continue;

                    allTraversals.merge(linkId, 1, Integer::sum);
                    if (isCB) cbTraversals.merge(linkId, 1, Integer::sum);
                }
            }
        }

        return new LinkStats(allTraversals, cbTraversals);
    }

    // =========================================================================
    //  Step 2a – identify eligible under-estimated links
    // =========================================================================

    /**
     * A link is eligible when all three conditions hold:
     *   (a) enough traversals to trust the statistics
     *   (b) cross-border agents represent >= CROSSBORDER_SHARE_THRESHOLD
     *   (c) simulated flow is under the observed count by >= FLOW_UNDER_ESTIMATION_THRESHOLD
     */
    private Map<Id<Link>, LinkSignal> identifyUnderEstimatedLinks(LinkStats stats) {
        Map<Id<Link>, LinkSignal> signals = new HashMap<>();

        for (Map.Entry<Id<Link>, Integer> entry : stats.allTraversals.entrySet()) {
            Id<Link> linkId    = entry.getKey();
            int      totalTrav = entry.getValue();

            if (totalTrav < MIN_TRAVERSALS_PER_LINK) continue;

            float counts = countsProcessor.getLinkCounts(linkId);
            if (counts <= 0.0f) continue;

            double simFlow       = flowProcessor.getTotalLinkFlow(linkId) / Math.max(sampleSize, 1.0e-9);
            double relativeError = (counts - simFlow) / Math.max(counts, 1.0e-9); // positive = under

            if (relativeError <= FLOW_UNDER_ESTIMATION_THRESHOLD) continue;

            int    cbTrav  = stats.cbTraversals.getOrDefault(linkId, 0);
            double cbShare = cbTrav / (double) totalTrav;

            if (cbShare < CROSSBORDER_SHARE_THRESHOLD) continue;

            // Fractional increase in CB traversals needed: counts/flow - 1
            double neededShare = relativeError / Math.max(1.0 - relativeError, 1.0e-9);
            neededShare        = Math.min(neededShare, 1.0);
            double dampingFactor = getDampingFactor();
            double dampedShare = neededShare * dampingFactor;

            if (dampedShare > 0.0) {
                signals.put(linkId, new LinkSignal(dampedShare, relativeError, cbShare));
            }
        }

        return signals;
    }

    private double getDampingFactor(){
        if (numExpansions ==0) {
            return 0.8;
        } else {
            double progress = clamp(1.0-numExpansions / 50.0, 0.1, 1.0);
            return Math.max(INITIAL_EXPANSION_DAMPING_FACTOR * progress,0.3);
        }
    }
    // =========================================================================
    //  Step 2b – build per-person clone probabilities (trip-level deduplication)
    // =========================================================================

    /**
     * Translates link-level signals into per-person clone probabilities while
     * correctly handling correlated counting stations via 1/N link-weighting.
     * The MAX is taken across a person's trips so a single clone fixes all trips.
     */
    private Map<Id<Person>, Double> buildDonorProbabilities(Map<Id<Link>, LinkSignal> signals) {
        Map<Id<Person>, Double> result = new HashMap<>();

        for (Person person : population.getPersons().values()) {
            if (isAlreadyCloned(person)) continue;
            if (!isCrossBorderPerson(person)) continue;

            Plan plan = person.getSelectedPlan();
            if (plan == null) continue;

            double maxTripProbability = 0.0;

            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(plan)) {
                if (!isCarOrTruck(trip)) continue;

                double tripProbability = computeTripCloneProbability(trip, signals);
                maxTripProbability = Math.max(maxTripProbability, tripProbability);
            }

            if (maxTripProbability > MIN_CLONE_PROBABILITY) {
                double finalProbability = clamp(maxTripProbability,
                        MIN_CLONE_PROBABILITY, MAX_CLONE_PROBABILITY);
                result.put(person.getId(), finalProbability);
            }
        }

        return result;
    }

    /**
     * Computes the clone probability for a single trip using 1/N correlated-station
     * weighting: N eligible links on one trip each contribute neededShare/N, so
     * N correlated stations give the same total weight as 1 isolated station.
     */
    private double computeTripCloneProbability(DiscreteModeChoiceTrip trip,
                                               Map<Id<Link>, LinkSignal> signals) {
        List<Id<Link>> routeLinks = extractRouteLinkIds(trip);
        if (routeLinks.isEmpty()) return 0.0;

        Set<Id<Link>> eligibleLinksOnTrip = new LinkedHashSet<>();
        for (Id<Link> linkId : routeLinks) {
            if (signals.containsKey(linkId)) {
                eligibleLinksOnTrip.add(linkId);
            }
        }

        if (eligibleLinksOnTrip.isEmpty()) return 0.0;

        int    N         = eligibleLinksOnTrip.size();
        double tripScore = 0.0;

        for (Id<Link> linkId : eligibleLinksOnTrip) {
            LinkSignal signal = signals.get(linkId);
            tripScore += signal.neededShare / N;
        }

        return Math.min(tripScore, MAX_CLONE_PROBABILITY);
    }

    // =========================================================================
    //  Step 3 – stochastically clone donors and re-route
    // =========================================================================

    /**
     * Iterates over eligible donors and, for each one drawn by the random roll,
     * creates a clone, registers its vehicles, re-routes its plan, and adds it
     * to the population.
     */
    private int applyCloning(Map<Id<Person>, Double> donorProbabilities, PlanRouter planRouter, int maxClones) {
        if (maxClones <= 0) {
            return 0;
        }

        int cloned = 0;

        // Snapshot to avoid ConcurrentModificationException while adding persons
        List<Map.Entry<Id<Person>, Double>> entries = new ArrayList<>(donorProbabilities.entrySet());

        for (Map.Entry<Id<Person>, Double> entry : entries) {
            if (cloned >= maxClones) {
                break;
            }
            if (random.nextDouble() >= entry.getValue()) continue;

            Person donor = population.getPersons().get(entry.getKey());
            if (donor == null) continue;

            Person clone = createClone(donor, planRouter);
            if (clone != null) {
                population.addPerson(clone);
                cloned++;
            }
        }

        return cloned;
    }

    private int restoreRemovedCrossBorderPersons(Map<Id<Link>, LinkSignal> signals,
                                                 Map<Id<Person>, Double> donorProbabilities) {
        if (subPopulationReducer == null || signals.isEmpty() || donorProbabilities.isEmpty()) {
            return 0;
        }

        int requestedRestores = estimateExpansionNeed(donorProbabilities);
        if (requestedRestores <= 0) {
            return 0;
        }

        Map<Id<Link>, Double> linkWeights = new HashMap<>();
        for (Map.Entry<Id<Link>, LinkSignal> signal : signals.entrySet()) {
            linkWeights.put(signal.getKey(), signal.getValue().neededShare);
        }

        int restored = subPopulationReducer.restoreCrossBorderPersonsForExpansion(linkWeights, requestedRestores);
        if (restored > 0) {
            logger.info("CrossBorderPopulationExpander: restored {} previously removed cross-border persons before cloning.", restored);
        }

        return restored;
    }

    private int estimateExpansionNeed(Map<Id<Person>, Double> donorProbabilities) {
        double expectedClones = donorProbabilities.values().stream().mapToDouble(Double::doubleValue).sum();
        return (int) Math.ceil(expectedClones);
    }

    // =========================================================================
    //  Cloning pipeline
    // =========================================================================

    /**
     * Full cloning pipeline:
     *   1. Build unique ID
     *   2. Copy attributes
     *   3. Register vehicles (must happen before routing so the router can
     *      resolve the vehicle and its type)
    *   4. Clone and relocate the plan
    *   5. Re-route car/truck trips in the plan using PlanRouter
     * Returns null if the clone cannot be validly constructed.
     */
    private Person createClone(Person donor, PlanRouter planRouter) {
        Plan donorPlan = donor.getSelectedPlan();
        if (donorPlan == null) {
            return null;
        }

        Id<Person> cloneId = buildCloneId(donor);

        Person clone = population.getFactory().createPerson(cloneId);
        copyPersonAttributes(donor, clone);
        clone.getAttributes().putAttribute(CLONED_ATTRIBUTE, true);

        // Vehicles must be registered before routing so the PlanRouter / departure
        // handler can resolve clone's vehicle ID and type.
        registerCloneVehicles(donor, clone);
        if (!validateCloneVehicleIsolation(donor, clone)) {
            return null;
        }

        Plan clonedPlan = clonePlan(donorPlan, clone);
        if (clonedPlan == null) return null;

        clone.addPlan(clonedPlan);
        clone.setSelectedPlan(clonedPlan);

        // Re-route after relocation so every leg has a valid route from the new
        // activity coordinates.
        boolean routedSuccessfully = reroutePlan(clone, clonedPlan, planRouter);
        if (!routedSuccessfully) {
            logger.warn("CrossBorderPopulationExpander: dropping clone {} because rerouting produced an invalid plan.",
                    clone.getId());
            return null;
        }

        return clone;
    }

    // ── ID generation ─────────────────────────────────────────────────────────

    protected Id<Person> buildCloneId(Person donor) {
        String prefix = donor.getId().toString() + CLONE_ID_SUFFIX;
        Id<Person> cloneId;
        do {
            cloneId = Id.createPersonId(prefix + (++cloneCounter));
        } while (population.getPersons().containsKey(cloneId));
        return cloneId;
    }

    // ── Vehicle registration ───────────────────────────────────────────────────

    /**
     * Creates and registers a fresh vehicle for the clone for every mode that
     * the donor already has a vehicle for.
     *
     * The clone's vehicle ID follows MATSim's default convention
     * {@code "<personId>:<mode>"} so that the router and departure handler
     * resolve it automatically. The donor's vehicle type is reused so traffic
     * dynamics are unchanged.
     */
    protected void registerCloneVehicles(Person donor, Person clone) {
        Map<String, Id<Vehicle>> donorVehicleIds = VehicleUtils.getVehicleIds(donor);
        Set<String> modesToRegister = new LinkedHashSet<>(donorVehicleIds.keySet());
        Map<String, Id<Vehicle>> cloneVehicleIds = new HashMap<>();

        Plan donorPlan = donor.getSelectedPlan();
        if (donorPlan != null) {
            for (DiscreteModeChoiceTrip trip : tripListConverter.convert(donorPlan)) {
                if (TransportMode.car.equals(trip.getInitialMode()) || TransportMode.truck.equals(trip.getInitialMode())) {
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
                logger.warn("CrossBorderPopulationExpander: skipping clone {} of donor {} because both use same {} vehicle id {}.",
                        clone.getId(), donor.getId(), mode, cloneVehicleId);
                return false;
            }
        }

        return true;
    }

    // ── Attribute copying ─────────────────────────────────────────────────────

    protected void copyPersonAttributes(Person donor, Person clone) {
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

    // ── Plan cloning ─────────────────────────────────────────────────────────

    /**
    * Builds a plan from the donor plan with cloned/relocated activities and
    * copied legs. Car/truck trips are re-routed later with PlanRouter; non-car
    * trips keep donor semantics and routes.
     *
     * Returns null if the donor plan has no activities (degenerate case).
     */
    protected Plan clonePlan(Plan donorPlan, Person cloneOwner) {
        if (donorPlan == null) return null;

        Plan newPlan = population.getFactory().createPlan();
        newPlan.setPerson(cloneOwner);

        for (PlanElement element : donorPlan.getPlanElements()) {
            if (element instanceof Activity activity) {
                newPlan.addActivity(cloneActivity(activity));
            } else if (element instanceof Leg leg) {
                newPlan.addLeg(cloneLeg(leg));
            }
        }

        if (newPlan.getPlanElements().isEmpty()) return null;
        newPlan.setScore(donorPlan.getScore());
        return newPlan;
    }

    protected Leg cloneLeg(Leg donorLeg) {
        Leg copy = population.getFactory().createLeg(donorLeg.getMode());
        copy.setRoutingMode(donorLeg.getRoutingMode() != null ? donorLeg.getRoutingMode() : donorLeg.getMode());
        donorLeg.getDepartureTime().ifDefined(copy::setDepartureTime);
        donorLeg.getTravelTime().ifDefined(copy::setTravelTime);
        copy.setRoute(donorLeg.getRoute());
        return copy;
    }

    /**
     * Clones a single activity and optionally relocates it.
     * Anchor activities ("outside") keep their original coord, facility, and link.
     * All other activities are snapped to a random real facility within
     * RELOCATION_RADIUS_METERS, and their link is updated to the nearest network
     * link of the chosen facility.
     */
    protected Activity cloneActivity(Activity original) {
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
            baseCoord = findNearestFacility(new Coord(0.0, 0.0)).getCoord();
        }

        Activity clone = population.getFactory().createActivityFromCoord(
                original.getType(), baseCoord);

        original.getStartTime().ifDefined(clone::setStartTime);
        original.getEndTime().ifDefined(clone::setEndTime);
        original.getMaximumDuration().ifDefined(clone::setMaximumDuration);

        if (!isAnchorActivity(original)) {
            ActivityFacility newFacility = relocateActivity(baseCoord);
            clone.setCoord(newFacility.getCoord());
            clone.setFacilityId(newFacility.getId());
            // Snap to nearest network link so the router has a valid start/end link.
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

    // ── Re-routing ────────────────────────────────────────────────────────────

    protected boolean reroutePlan(Person clone, Plan plan, PlanRouter planRouter) {
        try {
            // Re-route only motorized network trips using the same router stack as scenario preparation.
            planRouter.run(plan, true, NETWORK_ROUTING_MODES);
        } catch (Exception e) {
            logger.warn("CrossBorderPopulationExpander: PlanRouter failed for clone {}: {}", clone.getId(), e.getMessage());
            return false;
        }

        // Ensure every network route leg carries clone's vehicle id for its routing mode.
        if (!stampCloneVehicleIds(clone, plan)) {
            return false;
        }

        return hasConsistentRoutingModes(plan);
    }

    private boolean stampCloneVehicleIds(Person clone, Plan plan) {
        for (PlanElement element : plan.getPlanElements()) {
            if (!(element instanceof Leg leg) || !(leg.getRoute() instanceof NetworkRoute route)) {
                continue;
            }

            String routingMode = leg.getRoutingMode() != null ? leg.getRoutingMode() : leg.getMode();
            Id<Vehicle> vehicleId = VehicleUtils.getVehicleId(clone, routingMode);
            if (vehicleId == null) {
                logger.warn("CrossBorderPopulationExpander: clone {} has no vehicle id for routed mode {}, dropping clone.",
                        clone.getId(), routingMode);
                return false;
            }
            route.setVehicleId(vehicleId);
        }

        return true;
    }

    private boolean hasConsistentRoutingModes(Plan plan) {
        String openTripRoutingMode = null;

        for (PlanElement element : plan.getPlanElements()) {
            if (element instanceof Activity) {
                openTripRoutingMode = null;
                continue;
            }

            if (element instanceof Leg leg) {
                String routingMode = leg.getRoutingMode() != null ? leg.getRoutingMode() : leg.getMode();
                if (openTripRoutingMode == null) {
                    openTripRoutingMode = routingMode;
                } else if (!openTripRoutingMode.equals(routingMode)) {
                    logger.warn("CrossBorderPopulationExpander: inconsistent routing modes in one trip for clone {} ({} vs {}), dropping clone.",
                            plan.getPerson() != null ? plan.getPerson().getId() : "unknown", openTripRoutingMode, routingMode);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Derives the departure time from an activity's end time, start time, or 0.0.
     * Never calls .seconds() on an undefined OptionalTime.
     */
    private double departureTimeFrom(Activity activity) {
        if (activity.getEndTime().isDefined()) return activity.getEndTime().seconds();
        if (activity.getStartTime().isDefined()) return activity.getStartTime().seconds();
        return 0.0;
    }

    // ── Activity relocation ───────────────────────────────────────────────────

    /**
     * Returns a facility for the relocated activity, always snapping to an
     * existing facility (never a bare geometric point).
     */
    protected ActivityFacility relocateActivity(Coord original) {
        List<ActivityFacility> candidates = findFacilitiesWithinRadius(original, RELOCATION_RADIUS_METERS);
        if (!candidates.isEmpty()) {
            return selectFacilityCoord(candidates);
        }
        return findNearestFacility(original);
    }

    protected List<ActivityFacility> findFacilitiesWithinRadius(Coord center, double radiusMeters) {
        List<ActivityFacility> result = new ArrayList<>();
        double radiusSq = radiusMeters * radiusMeters;

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            Coord  fc = facility.getCoord();
            double dx = fc.getX() - center.getX();
            double dy = fc.getY() - center.getY();
            if (dx * dx + dy * dy <= radiusSq) {
                result.add(facility);
            }
        }

        return result;
    }

    protected ActivityFacility selectFacilityCoord(List<ActivityFacility> candidates) {
        return candidates.get(random.nextInt(candidates.size()));
    }

    protected ActivityFacility findNearestFacility(Coord center) {
        ActivityFacility nearest   = null;
        double           minDistSq = Double.POSITIVE_INFINITY;

        for (ActivityFacility facility : facilities.getFacilities().values()) {
            Coord  fc     = facility.getCoord();
            double dx     = fc.getX() - center.getX();
            double dy     = fc.getY() - center.getY();
            double distSq = dx * dx + dy * dy;
            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest   = facility;
            }
        }

        if (nearest == null) {
            throw new IllegalStateException(
                    "CrossBorderPopulationExpander: scenario has no facilities — cannot relocate activity.");
        }

        return nearest;
    }

    // =========================================================================
    //  Predicates
    // =========================================================================

    protected boolean isCrossBorderPerson(Person person) {
        Boolean isCB = (Boolean) person.getAttributes().getAttribute("isCrossBorder");
        return isCB != null && isCB;
    }

    protected boolean isAlreadyCloned(Person person) {
        Object cloned = person.getAttributes().getAttribute(CLONED_ATTRIBUTE);
        return Boolean.TRUE.equals(cloned);
    }

    protected boolean isCarOrTruck(DiscreteModeChoiceTrip trip) {
        return TransportMode.car.equals(trip.getInitialMode())
                || TransportMode.truck.equals(trip.getInitialMode());
    }

    protected boolean isAnchorActivity(Activity activity) {
        return ANCHOR_ACTIVITY_TYPE.equals(activity.getType());
    }

    // =========================================================================
    //  Route extraction
    // =========================================================================

    private List<Id<Link>> extractRouteLinkIds(DiscreteModeChoiceTrip trip) {
        List<Id<Link>> linkIds = new ArrayList<>();
        for (PlanElement element : trip.getInitialElements()) {
            if (element instanceof Leg leg && leg.getRoute() instanceof NetworkRoute route) {
                linkIds.addAll(route.getLinkIds());
            }
        }
        return linkIds;
    }

    // =========================================================================
    //  Utility
    // =========================================================================

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    // =========================================================================
    //  Internal data records
    // =========================================================================

    private record LinkStats(
            Map<Id<Link>, Integer> allTraversals,
            Map<Id<Link>, Integer> cbTraversals) {
    }

    private record LinkSignal(
            double neededShare,
            double relativeError,
            double cbShare) {
    }
}