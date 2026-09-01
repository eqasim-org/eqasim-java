package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.crossborder;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class TestCrossBorderCloneFactory {
    private final PopulationFactory factory = ScenarioUtils.createScenario(ConfigUtils.createConfig())
            .getPopulation().getFactory();

    @Test
    public void borderAndOutsideActivitiesAreFixedCaseInsensitively() {
        assertTrue(CrossBorderActivityRules.isFixedLocation(activity("border")));
        assertTrue(CrossBorderActivityRules.isFixedLocation(activity(" Border ")));
        assertTrue(CrossBorderActivityRules.isFixedLocation(activity("OUTSIDE")));
        assertFalse(CrossBorderActivityRules.isFixedLocation(activity("work")));
    }

    @Test
    public void anyOneSecondActivityIsTreatedAsAFixedConnector() {
        Activity maximumDurationConnector = activity("interaction");
        maximumDurationConnector.setMaximumDuration(1.0);
        assertTrue(CrossBorderActivityRules.isFixedLocation(maximumDurationConnector));

        Activity timedConnector = activity("interaction");
        timedConnector.setMaximumDuration(10.0);
        timedConnector.setStartTime(100.0);
        timedConnector.setEndTime(101.0);
        assertTrue(CrossBorderActivityRules.isFixedLocation(timedConnector));
    }

    @Test
    public void planMetadataCanBeCopiedWithoutPlanInheritanceAttributes() {
        Plan source = factory.createPlan();
        source.setScore(12.5);
        source.setType("crossborder");
        source.getAttributes().putAttribute("custom", "value");
        Plan target = factory.createPlan();

        CrossBorderCloneFactory.copyPlanMetadata(source, target);

        assertEquals(12.5, target.getScore(), 1.0e-9);
        assertEquals("crossborder", target.getType());
        assertEquals("value", target.getAttributes().getAttribute("custom"));
        assertNull(target.getAttributes().getAttribute("iterationCreated"));
    }

    @Test
    public void negativeShiftIsConstrainedUniformlyAtZero() {
        Plan plan = factory.createPlan();
        Activity first = activity("home");
        first.setEndTime(300.0);
        Leg leg = factory.createLeg("car");
        leg.setDepartureTime(300.0);
        Activity second = activity("work");
        second.setStartTime(600.0);
        plan.addActivity(first);
        plan.addLeg(leg);
        plan.addActivity(second);

        assertEquals(-300, CrossBorderCloneFactory.constrainTimeShiftToNonNegativeTimes(
                plan, -600));
        assertEquals(120, CrossBorderCloneFactory.constrainTimeShiftToNonNegativeTimes(
                plan, 120));
    }

    @Test
    public void legDepartureIsSynchronizedWithPrecedingActivityEnd() {
        Plan plan = factory.createPlan();
        Activity first = activity("home");
        first.setEndTime(1_200.0);
        Leg leg = factory.createLeg("car");
        leg.setDepartureTime(600.0);
        Activity second = activity("work");
        plan.addActivity(first);
        plan.addLeg(leg);
        plan.addActivity(second);

        CrossBorderCloneFactory.synchronizeLegDeparturesWithActivities(plan);

        assertEquals(1_200.0, leg.getDepartureTime().seconds(), 1.0e-9);
    }

    @Test
    public void relocatedActivityUsesFacilityLinkInsteadOfNearestLink() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        NetworkFactory networkFactory = scenario.getNetwork().getFactory();
        Node first = networkFactory.createNode(Id.createNodeId("first"), new Coord(0.0, 0.0));
        Node second = networkFactory.createNode(Id.createNodeId("second"), new Coord(100.0, 0.0));
        Node third = networkFactory.createNode(Id.createNodeId("third"), new Coord(200.0, 0.0));
        scenario.getNetwork().addNode(first);
        scenario.getNetwork().addNode(second);
        scenario.getNetwork().addNode(third);

        Link facilityLink = networkFactory.createLink(Id.createLinkId("facility-link"), first, second);
        Link nearestLink = networkFactory.createLink(Id.createLinkId("nearest-link"), second, third);
        scenario.getNetwork().addLink(facilityLink);
        scenario.getNetwork().addLink(nearestLink);

        ActivityFacility facility = scenario.getActivityFacilities().getFactory().createActivityFacility(
                Id.create("facility", ActivityFacility.class), new Coord(190.0, 0.0), facilityLink.getId());

        assertEquals(nearestLink.getId(), NetworkUtils.getNearestLink(
                scenario.getNetwork(), facility.getCoord()).getId());
        assertEquals(facilityLink.getId(), CrossBorderCloneFactory.linkForRelocatedActivity(
                facility, scenario.getNetwork()));
    }

    private Activity activity(String type) {
        return factory.createActivityFromCoord(type, new Coord(10.0, 20.0));
    }
}
