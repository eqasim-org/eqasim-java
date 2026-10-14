package org.eqasim.switzerland.ch_cmdp.memory;

import java.util.List;
import java.util.Map;

import org.eqasim.switzerland.ch_cmdp.SwitzerlandConfigurator;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.mode_availability.SwissDetailedModeAvailability;
import org.eqasim.switzerland.ch_cmdp.mode_choice.utilities.predictors.SwissPredictorUtils;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;

import static org.junit.Assert.*;

public class TestScenarioAttributeCleaner {
    private static CommandLine command(String... args) throws ConfigurationException {
        return new CommandLine.Builder(args).allowOptions(ScenarioAttributeCleaner.OPTION).build();
    }

    @Test
    public void defaultsToEnabledAndCanBeDisabled() throws Exception {
        assertTrue(ScenarioAttributeCleaner.isEnabled(command()));
        assertTrue(ScenarioAttributeCleaner.isEnabled(command("--lighten-scenario", "true")));
        assertFalse(ScenarioAttributeCleaner.isEnabled(command("--lighten-scenario", "false")));
        assertThrows(ConfigurationException.class, () ->
                ScenarioAttributeCleaner.isEnabled(command("--lighten-scenario", "typo")));
    }

    @Test
    public void keepsHouseholdDerivedModeChoiceInputsAndVehicleAssignments() throws Exception {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var factory = scenario.getPopulation().getFactory();
        var person = factory.createPerson(Id.createPersonId("person"));
        person.getAttributes().putAttribute("age", 40);
        person.getAttributes().putAttribute("hasLicense", "yes");
        person.getAttributes().putAttribute("carAvail", "always");
        person.getAttributes().putAttribute("bikeAvail", "always");
        person.getAttributes().putAttribute("hasWalkLoopTrip", true);
        person.getAttributes().putAttribute("subscriptions", "GA");
        person.getAttributes().putAttribute("carASC", 1.5);
        person.getAttributes().putAttribute("unusedPersonMetadata", "discard");
        var plan = factory.createPlan();
        var home = factory.createActivityFromCoord("home", new Coord(100, 200));
        plan.addActivity(home);
        var leg = factory.createLeg("car");
        TripStructureUtils.setRoutingMode(leg, "car");
        leg.getAttributes().putAttribute("unusedLegMetadata", "discard");
        plan.addLeg(leg);
        var work = factory.createActivityFromCoord("work", new Coord(300, 400));
        work.getAttributes().putAttribute("municipalityType", "urban");
        work.getAttributes().putAttribute("employeeDensity", 12.0);
        work.getAttributes().putAttribute("unusedActivityMetadata", "discard");
        plan.addActivity(work);
        plan.getAttributes().putAttribute("utility", -12.0);
        person.addPlan(plan);
        scenario.getPopulation().addPerson(person);
        var household = scenario.getHouseholds().getFactory().createHousehold(Id.create("household", org.matsim.households.Household.class));
        household.getMemberIds().add(person.getId());
        household.getAttributes().putAttribute("numberOfCars", "1");
        household.getAttributes().putAttribute("spRegion", 1);
        household.getAttributes().putAttribute("ovgk", "A");
        household.getAttributes().putAttribute("incomePerCapita", 4000.0);
        household.getAttributes().putAttribute("cantonId", 25.0);
        household.getAttributes().putAttribute("cantonName", "Geneva");
        scenario.getHouseholds().getHouseholds().put(household.getId(), household);
        new SwitzerlandConfigurator(command()).adjustScenario(scenario);
        var vehicle = VehicleUtils.createVehicle(Id.createVehicleId("car"), VehicleUtils.createDefaultVehicleType());
        VehicleUtils.insertVehicleIdsIntoAttributes(person, Map.of("car", vehicle.getId()));
        Object assignments = person.getAttributes().getAttribute("vehicles");
        var availability = new SwissDetailedModeAvailability();
        var modes = availability.getAvailableModes(person, List.of());

        assertTrue(ScenarioAttributeCleaner.clean(scenario) > 0);

        assertEquals(modes, availability.getAvailableModes(person, List.of()));
        assertTrue(SwissPredictorUtils.hasGeneralSubscription(person));
        assertEquals(4000.0, SwissPredictorUtils.getIncomePerCapita(person), 0.0);
        assertEquals(0.0, SwissPredictorUtils.getCarOwnershipRatio(person), 0.0);
        assertEquals(1.5, SwissPredictorUtils.getCarASC(person), 0.0);
        assertSame(assignments, person.getAttributes().getAttribute("vehicles"));
        assertEquals(vehicle.getId(), VehicleUtils.getVehicleIds(person).get("car"));
        assertEquals("car", TripStructureUtils.getRoutingMode(leg));
        assertEquals("urban", work.getAttributes().getAttribute("municipalityType"));
        assertEquals(12.0, (Double) work.getAttributes().getAttribute("employeeDensity"), 0.0);
        assertEquals(-12.0, (Double) plan.getAttributes().getAttribute("utility"), 0.0);
        assertSame(plan, person.getSelectedPlan());
        assertTrue(household.getAttributes().isEmpty());
        assertTrue(household.getMemberIds().contains(person.getId()));
        assertNull(person.getAttributes().getAttribute("unusedPersonMetadata"));
        assertNull(leg.getAttributes().getAttribute("unusedLegMetadata"));
        assertNull(work.getAttributes().getAttribute("unusedActivityMetadata"));
        // Restart preparation with empty household attributes must preserve person data.
        person.getAttributes().putAttribute("carOwnershipRatio", 0.75);
        new SwitzerlandConfigurator(command()).adjustScenario(scenario);
        assertEquals(0.75, SwissPredictorUtils.getCarOwnershipRatio(person), 0.0);
        person.getAttributes().removeAttribute("cantonName");
        var error = assertThrows(IllegalArgumentException.class,
                () -> new SwitzerlandConfigurator(command()).adjustScenario(scenario));
        assertTrue(error.getMessage().contains("cantonName"));
        assertTrue(error.getMessage().contains("person"));
        assertTrue(error.getMessage().contains("household"));
        assertEquals(0, ScenarioAttributeCleaner.clean(scenario));
    }

    @Test
    public void consolidatesMatchingAndLegacyOnlyHomeCoordinatesButKeepsConflicts() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        for (int i = 0; i < 3; i++) {
            var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(i));
            scenario.getPopulation().addPerson(person);
            person.getAttributes().putAttribute("home_coordinate_x", 100.0);
            person.getAttributes().putAttribute("home_coordinate_y", 200.0);
            if (i != 1) {
                person.getAttributes().putAttribute("home_x", i == 0 ? 100.0 : 0.0);
                person.getAttributes().putAttribute("home_y", i == 0 ? 200.0 : 0.0);
            }
        }
        assertEquals(4, ScenarioAttributeCleaner.clean(scenario));
        for (int i = 0; i < 3; i++) {
            var person = scenario.getPopulation().getPersons().get(Id.createPersonId(i));
            assertEquals(i == 2 ? 0.0 : 100.0, (Double) person.getAttributes().getAttribute("home_x"), 0.0);
            assertEquals(i == 2 ? 0.0 : 200.0, (Double) person.getAttributes().getAttribute("home_y"), 0.0);
            if (i == 2) assertEquals(100.0, person.getAttributes().getAttribute("home_coordinate_x"));
            else assertNull(person.getAttributes().getAttribute("home_coordinate_x"));
        }
        assertEquals(0, ScenarioAttributeCleaner.clean(scenario));
    }

    @Test
    public void keepsRoutingCalibrationAndTransitOperationalData() {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork();
        var a = network.getFactory().createNode(Id.createNodeId("a"), new Coord(0, 0));
        var b = network.getFactory().createNode(Id.createNodeId("b"), new Coord(100, 0));
        network.addNode(a);
        network.addNode(b);
        var link = network.getFactory().createLink(Id.createLinkId("link"), a, b);
        network.addLink(link);
        link.getAttributes().putAttribute("osm:way:highway", "primary");
        link.getAttributes().putAttribute("osm:way:name", "Main street");
        link.getAttributes().putAttribute("municipalityType", "urban");
        link.getAttributes().putAttribute("penalty", 0.2);
        link.getAttributes().putAttribute("speedFactor", 0.9);
        link.getAttributes().putAttribute("traffic_light", true);
        link.getAttributes().putAttribute("toll", 2.0);
        link.getAttributes().putAttribute("accesstime_car", 5.0);
        org.matsim.core.network.NetworkUtils.setDisallowedNextLinks(link,
                new org.matsim.core.network.turnRestrictions.DisallowedNextLinks());
        Object restrictions = link.getAttributes().getAttribute("disallowedNextLinks");
        link.getAttributes().putAttribute("osm:way:id", "unused");
        a.getAttributes().putAttribute("unusedNodeMetadata", "unused");
        var stop = scenario.getTransitSchedule().getFactory().createTransitStopFacility(
                Id.create("stop", org.matsim.pt.transitSchedule.api.TransitStopFacility.class), new Coord(0, 0), false);
        scenario.getTransitSchedule().addStopFacility(stop);
        org.matsim.pt.transitSchedule.TransitScheduleUtils.setStopAccessTime(stop, 15.0);
        stop.getAttributes().putAttribute("stopCategory", 2);
        var type = VehicleUtils.createDefaultVehicleType();
        type.setAccessTime(0.5);
        type.setEgressTime(0.25);
        type.setDoorOperationMode(org.matsim.vehicles.VehicleType.DoorOperationMode.parallel);
        scenario.getTransitVehicles().addVehicleType(type);
        var bus = VehicleUtils.createVehicle(Id.createVehicleId("bus"), type);
        scenario.getTransitVehicles().addVehicle(bus);
        bus.getAttributes().putAttribute("age", 10);
        bus.getAttributes().putAttribute("euro", "6");

        ScenarioAttributeCleaner.clean(scenario);

        assertEquals("primary", link.getAttributes().getAttribute("osm:way:highway"));
        assertEquals("Main street", link.getAttributes().getAttribute("osm:way:name"));
        assertEquals("urban", link.getAttributes().getAttribute("municipalityType"));
        assertEquals(0.2, (Double) link.getAttributes().getAttribute("penalty"), 0.0);
        assertEquals(0.9, (Double) link.getAttributes().getAttribute("speedFactor"), 0.0);
        assertEquals(true, link.getAttributes().getAttribute("traffic_light"));
        assertEquals(2.0, (Double) link.getAttributes().getAttribute("toll"), 0.0);
        assertSame(restrictions, link.getAttributes().getAttribute("disallowedNextLinks"));
        assertEquals(5.0, (Double) link.getAttributes().getAttribute("accesstime_car"), 0.0);
        assertEquals(15.0, org.matsim.pt.transitSchedule.TransitScheduleUtils.getStopAccessTime(stop), 0.0);
        assertEquals(0.5, type.getAccessTime(), 0.0);
        assertEquals(0.25, type.getEgressTime(), 0.0);
        assertEquals(org.matsim.vehicles.VehicleType.DoorOperationMode.parallel, type.getDoorOperationMode());
        assertTrue(bus.getAttributes().isEmpty());
        assertTrue(a.getAttributes().isEmpty());
        assertNull(link.getAttributes().getAttribute("osm:way:id"));
        assertNull(stop.getAttributes().getAttribute("stopCategory"));
    }

    @Test
    public void retainsAttributeNamesSelectedInNestedConfiguration() {
        var config = ConfigUtils.createConfig();
        var policies = new ConfigGroup("eqasim:policies");
        var policy = new ConfigGroup("discount");
        policy.addParam("personFilter", "!eligibleForDiscount");
        policies.addParameterSet(policy);
        config.addModule(policies);
        var routing = new ConfigGroup("customRouting");
        routing.addParam("personSpeedAttribute", "walkingSpeed");
        routing.addParam("attributeName", "restrictedArea");
        config.addModule(routing);
        var scenario = ScenarioUtils.createScenario(config);
        var person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("person"));
        scenario.getPopulation().addPerson(person);
        person.getAttributes().putAttribute("eligibleForDiscount", true);
        person.getAttributes().putAttribute("walkingSpeed", 1.2);
        person.getAttributes().putAttribute("restrictedArea", false);
        person.getAttributes().putAttribute("unconfigured", true);

        ScenarioAttributeCleaner.clean(scenario);

        assertEquals(true, person.getAttributes().getAttribute("eligibleForDiscount"));
        assertEquals(1.2, (Double) person.getAttributes().getAttribute("walkingSpeed"), 0.0);
        assertEquals(false, person.getAttributes().getAttribute("restrictedArea"));
        assertNull(person.getAttributes().getAttribute("unconfigured"));
    }
}
