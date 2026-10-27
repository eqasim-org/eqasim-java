package org.eqasim.switzerland.ch_cmdp.memory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.eqasim.switzerland.ch_cmdp.transit_dwell.SampledTransitDwellConfigGroup;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.attributable.Attributes;
import org.matsim.vehicles.Vehicles;

/**
 * Removes unused input metadata for the ch_cmdp runner, after adjustScenario has
 * copied household data to persons. Keep these lists in sync with new consumers.
 * Simulation and routing attributes are retained alongside mode-choice and
 * calibration inputs. Duplicate home-coordinate aliases are consolidated.
 */
public final class ScenarioAttributeCleaner {
    public static final String OPTION = "lighten-scenario";
    private static final Logger logger = LogManager.getLogger(ScenarioAttributeCleaner.class);

    // SwissPredictorUtils, SwissDetailedModeAvailability, calibration Tools and
    // MATSim scoring/replanning/vehicle assignment; flags also feed final statistics.
    private static final Set<String> PERSON = Set.of(
            "age", "sex", "hasLicense", "carAvail", "bikeAvail", "subscriptions",
            "spRegion", "incomePerCapita", "carOwnershipRatio", "cantonId", "cantonName",
            "ovgk", "numberOfCars", "home_x", "home_y", "home_coordinate_x", "home_coordinate_y",
            "carASC", "isFreight", "isCrossBorder", "isExternalFR", "isOutside", "outside",
            "hasWalkLoopTrip", "hasPtLoopTrip", "hasBikeLoopTrip", "hasCarLoopTrip",
            "hasCarPassengerLoopTrip", "subpopulation", "vehicles", "vehicleTypes",
            "income", "modeConstants", "modeTasteVariations");
    private static final Set<String> PLAN = Set.of(
            "utility", "createdLastIteration", "isInitialPlan", "planId", "iterationCreated",
            "planMutator", "initialPlan", "prebookingOffset_s");
    // Destination utility inputs and MATSim time-allocation mutation.
    private static final Set<String> ACTIVITY = Set.of(
            "municipalityType", "municipalityId", "employeeDensity", "companiesDensity",
            "populationDensity", "ovgk", "headway_min", "initialEndTime");
    private static final Set<String> LEG = Set.of(
            "routingMode", "enterVehicleTime", "vehicleId", "accessId", "currentSegmentType");
    // LinkCategorizer, IntersectionGroups, delay models, tolls and MATSim routing.
    private static final Set<String> LINK = Set.of(
            "osm:way:highway", "osm:way:name", "municipalityType", "speedFactor", "penalty",
            "traffic_light", "toll", "crossingPenalty", "disallowedNextLinks", "allowed_speed");
    private static final Set<String> STOP = Set.of("accessTime", "egressTime");
    private static final Set<String> VEHICLE = Set.of("initialLinkId");
    private static final Set<String> VEHICLE_TYPE = Set.of(
            "accessTimeInSecondsPerPerson", "egressTimeInSecondsPerPerson", "doorOperationMode");
    private static final Set<String> ENGINE = Set.of(
            "HbefaVehicleCategory", "HbefaTechnology", "HbefaSizeClass", "HbefaEmissionsConcept",
            "fuelConsumptionLitersPerMeter", "energyConsumptionKWhPerMeter",
            "energyCapacityInKWhOrLiters", "fuelType");
    private static final Set<String> COST = Set.of("costsPerSecondWaiting", "costsPerSecondInService");
    // Attribute names can also be selected in config parameter sets. Preserve
    // these keys on every object type, including raw or optional config groups.
    private static final Set<String> ATTRIBUTE_NAME_PARAMETERS = Set.of(
            "attributeName", "personFilter", "personFilterAttribute", "stopFilterAttribute",
            "linkIdAttribute", "personSpeedAttribute", "subpopulationAttributeName");

    private ScenarioAttributeCleaner() { }

    public static boolean isEnabled(CommandLine cmd) throws ConfigurationException {
        String value = cmd.getOption(OPTION).orElse("true");
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new ConfigurationException("--" + OPTION + " must be true or false: " + value);
    }

    /** Returns the number of removed attribute entries; call before controller construction. */
    public static long clean(Scenario scenario) {
        return clean(scenario, null);
    }

    /** Cleans as usual, saving removed stable-object metadata for final output restoration. */
    public static ScenarioAttributeArchive cleanAndArchive(Scenario scenario) throws IOException {
        return cleanAndArchive(scenario, null);
    }

    public static ScenarioAttributeArchive cleanAndArchive(Scenario scenario, Path temporaryParent) throws IOException {
        try (var archive = new ScenarioAttributeArchive.Builder()) {
            clean(scenario, archive);
            return archive.finish(scenario, temporaryParent);
        } catch (UncheckedIOException e) { throw e.getCause(); }
    }

    private static long clean(Scenario scenario, ScenarioAttributeArchive.Builder archive) {
        Set<String> configured = new HashSet<>();
        scenario.getConfig().getModules().values().forEach(group -> collectConfiguredAttributes(group, configured));
        Map<String, Long> removed = new TreeMap<>();

        scenario.getPopulation().getPersons().values().forEach(person -> {
            consolidateHomeCoordinates(person.getAttributes(), configured, removed, archive, person.getId().toString());
            retain(person.getAttributes(), PERSON, configured, "persons", removed, archive,
                    ScenarioAttributeArchive.PERSON, person.getId().toString());
            person.getPlans().forEach(plan -> {
                retain(plan.getAttributes(), PLAN, configured, "plans", removed);
                plan.getPlanElements().forEach(element -> {
                    if (element instanceof Activity activity) {
                        retain(activity.getAttributes(), ACTIVITY, configured, "activities", removed);
                    } else if (element instanceof Leg leg) {
                        retain(leg.getAttributes(), LEG, configured, "legs", removed);
                    }
                });
            });
        });
        scenario.getNetwork().getLinks().values().forEach(link ->
                retain(link.getAttributes(), LINK, configured, "links", removed, archive,
                        ScenarioAttributeArchive.LINK, link.getId().toString()));
        scenario.getNetwork().getNodes().values().forEach(node ->
                retain(node.getAttributes(), Set.of(), configured, "nodes", removed));
        // Household attributes have already been copied; preserve household membership and income fields.
        scenario.getHouseholds().getHouseholds().values().forEach(household ->
                retain(household.getAttributes(), Set.of(), configured, "households", removed, archive,
                        ScenarioAttributeArchive.HOUSEHOLD, household.getId().toString()));
        scenario.getActivityFacilities().getFacilities().values().forEach(facility ->
                retain(facility.getAttributes(), Set.of(), configured, "facilities", removed));
        scenario.getTransitSchedule().getFacilities().values().forEach(stop ->
                retain(stop.getAttributes(), STOP, configured, "transitStops", removed));
        scenario.getTransitSchedule().getTransitLines().values().forEach(line -> {
            retain(line.getAttributes(), Set.of(), configured, "transitLines", removed);
            line.getRoutes().values().forEach(route -> {
                retain(route.getAttributes(), Set.of(), configured, "transitRoutes", removed);
                route.getDepartures().values().forEach(departure ->
                        retain(departure.getAttributes(), Set.of(), configured, "departures", removed));
            });
        });
        cleanVehicles(scenario.getVehicles(), configured, "road", removed);
        cleanVehicles(scenario.getTransitVehicles(), configured, "transit", removed);
        // Top-level CRS/scale attributes are small and needed when writing the scenario.
        long total = removed.values().stream().mapToLong(Long::longValue).sum();
        logger.info("Scenario attribute cleanup removed {} entries", total);
        removed.forEach((name, count) -> logger.info("  {}: {}", name, count));
        return total;
    }

    private static void consolidateHomeCoordinates(Attributes attributes, Set<String> configured,
                                                   Map<String, Long> removed, ScenarioAttributeArchive.Builder archive, String personId) {
        Object legacyX = attributes.getAttribute("home_coordinate_x");
        Object legacyY = attributes.getAttribute("home_coordinate_y");
        if (!(legacyX instanceof Number x) || !(legacyY instanceof Number y)) return;
        Object normalX = attributes.getAttribute("home_x");
        Object normalY = attributes.getAttribute("home_y");
        // Calibration already falls back to the complete legacy pair if either
        // normal coordinate is absent. Make that pair available to mode choice too.
        if (normalX == null || normalY == null) {
            attributes.putAttribute("home_x", legacyX);
            attributes.putAttribute("home_y", legacyY);
            normalX = legacyX;
            normalY = legacyY;
        }
        if (normalX instanceof Number nx && normalY instanceof Number ny
                && nx.doubleValue() == x.doubleValue() && ny.doubleValue() == y.doubleValue()) {
            var aliases = new ArrayList<String>();
            for (String key : Set.of("home_coordinate_x", "home_coordinate_y")) if (!configured.contains(key)) aliases.add(key);
            if (archive != null) archive.capture(ScenarioAttributeArchive.PERSON, personId, attributes, aliases);
            for (String key : aliases) {
                attributes.removeAttribute(key);
                removed.merge("persons." + key, 1L, Long::sum);
            }
        }
        // Keep conflicting pairs: removing them would discard distinct input data.
    }

    private static void cleanVehicles(Vehicles vehicles, Set<String> configured, String category,
                                      Map<String, Long> removed) {
        vehicles.getVehicles().values().forEach(vehicle ->
                retain(vehicle.getAttributes(), VEHICLE, configured, category + "Vehicles", removed));
        vehicles.getVehicleTypes().values().forEach(type -> {
            retain(type.getAttributes(), VEHICLE_TYPE, configured, category + "VehicleTypes", removed);
            retain(type.getCapacity().getAttributes(), Set.of(), configured, category + "Capacities", removed);
            retain(type.getEngineInformation().getAttributes(), ENGINE, configured, category + "Engines", removed);
            retain(type.getCostInformation().getAttributes(), COST, configured, category + "Costs", removed);
        });
    }

    private static void retain(Attributes attributes, Set<String> required, Set<String> configured,
                               String category, Map<String, Long> removed) {
        retain(attributes, required, configured, category, removed, null, 0, null);
    }

    private static void retain(Attributes attributes, Set<String> required, Set<String> configured,
                               String category, Map<String, Long> removed, ScenarioAttributeArchive.Builder archive,
                               int kind, String id) {
        if (attributes.isEmpty()) return;
        // Attributes.getAsMap() is a view whose behavior is undefined during mutation.
        var unused = new ArrayList<String>();
        for (String name : attributes.getAsMap().keySet()) {
            boolean accessTime = "links".equals(category)
                    && (name.startsWith("accesstime_") || name.startsWith("egresstime_"));
            if (!required.contains(name) && !configured.contains(name) && !accessTime) {
                unused.add(name);
            }
        }
        if (archive != null) archive.capture(kind, id, attributes, unused);
        if (unused.isEmpty()) return;
        if (unused.size() == attributes.size()) {
            // clear() reuses AttributesImpl's shared empty arrays; removing keys
            // individually would leave two newly allocated zero-length arrays.
            attributes.clear();
        } else {
            unused.forEach(attributes::removeAttribute);
        }
        unused.forEach(name -> removed.merge(category + "." + name, 1L, Long::sum));
    }

    private static void collectConfiguredAttributes(ConfigGroup group, Set<String> names) {
        group.getParams().forEach((parameter, v) -> {
            if (ATTRIBUTE_NAME_PARAMETERS.contains(parameter) && v != null && !v.isBlank()
                    && !"null".equalsIgnoreCase(v)) {
                names.add("personFilter".equals(parameter) && v.startsWith("!") ? v.substring(1) : v);
            }
        });
        group.getParameterSets().values().forEach(groups ->
                groups.forEach(child -> collectConfiguredAttributes(child, names)));
    }

    /** Loads and prepares a scenario and logs cleanup results without running or writing a simulation. */
    public static void main(String[] args) throws ConfigurationException, IOException {
        CommandLine cmd = new CommandLine.Builder(args).requireOptions("config-path").build();
        SwitzerlandConfigurator configurator = new SwitzerlandConfigurator(cmd);
        var config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"), new SampledTransitDwellConfigGroup());
        configurator.updateConfig(config);
        configurator.configure(config);
        cmd.applyConfiguration(config);
        Scenario scenario = ScenarioUtils.createScenario(config);
        configurator.configureScenario(scenario);
        ScenarioUtils.loadScenario(scenario);
        configurator.adjustScenario(scenario);
        clean(scenario);
    }
}
