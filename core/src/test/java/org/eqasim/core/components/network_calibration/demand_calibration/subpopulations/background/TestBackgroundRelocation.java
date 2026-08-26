package org.eqasim.core.components.network_calibration.demand_calibration.subpopulations.background;

import com.google.inject.Provider;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacility;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestBackgroundRelocation {
    @Test
    public void crowFlyRadiusIsTwentyPercentClippedToConfiguredBounds() {
        assertEquals(200.0, BackgroundPlanRelocator.searchRadius(500.0, 0.20, 200.0, 5_000.0), 1.0e-9);
        assertEquals(2_000.0, BackgroundPlanRelocator.searchRadius(10_000.0, 0.20, 200.0, 5_000.0), 1.0e-9);
        assertEquals(5_000.0, BackgroundPlanRelocator.searchRadius(100_000.0, 0.20, 200.0, 5_000.0), 1.0e-9);
    }

    @Test
    public void repeatedCorrectionsRemainAnchoredToStartupEndpoints() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("freight"));
        person.getAttributes().putAttribute("isFreight", true);
        Plan original = plan(scenario, person, new Coord(0.0, 0.0), new Coord(10_000.0, 0.0));
        person.addPlan(original);
        person.setSelectedPlan(original);
        scenario.getPopulation().addPerson(person);
        BackgroundPlanRelocator.AnchorStore anchors = new BackgroundPlanRelocator.AnchorStore(
                scenario, 0.20, 200.0, 5_000.0);

        BackgroundPlanRelocator.FreightAnchor first = anchors.freight(person);
        Plan corrected = plan(scenario, person, new Coord(1_500.0, 0.0), new Coord(8_500.0, 0.0));
        person.addPlan(corrected);
        person.setSelectedPlan(corrected);
        person.removePlan(original);
        BackgroundPlanRelocator.FreightAnchor second = anchors.freight(person);

        assertEquals(0.0, second.origin().getX(), 1.0e-9);
        assertEquals(10_000.0, second.destination().getX(), 1.0e-9);
        assertEquals(2_000.0, second.radius(), 1.0e-9);
        assertEquals(first, second);
    }

    @Test
    public void crossBorderRelocationSelectsTheCentralSharedActivity() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("cross-border"));
        Plan plan = crossBorderPlan(scenario, person);
        var trips = TripStructureUtils.getTrips(plan);

        int activityIndex = BackgroundPlanRelocator.centralMovableActivityIndex(trips);
        Activity selected = BackgroundPlanRelocator.sharedActivity(trips, activityIndex);

        assertEquals(2, activityIndex);
        assertEquals("work", selected.getType());
        assertSame(trips.get(1).getDestinationActivity(), selected);
        assertSame(trips.get(2).getOriginActivity(), selected);
    }

    @Test
    public void movingSharedActivityUpdatesBothAdjacentTripEndpoints() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("cross-border"));
        Plan plan = crossBorderPlan(scenario, person);
        var trips = TripStructureUtils.getTrips(plan);
        Activity selected = BackgroundPlanRelocator.sharedActivity(trips, 2);

        selected.setCoord(new Coord(6_000.0, 2_000.0));

        assertEquals(6_000.0, trips.get(1).getDestinationActivity().getCoord().getX(), 1.0e-9);
        assertEquals(6_000.0, trips.get(2).getOriginActivity().getCoord().getX(), 1.0e-9);
        assertSame(trips.get(1).getDestinationActivity(), trips.get(2).getOriginActivity());
    }

    @Test
    public void crossBorderProposalMovesOnlyInlandActivityAndRoutesItsTwoTrips() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId("cross-border"));
        person.getAttributes().putAttribute("isCrossBorder", true);
        Plan source = crossBorderPlan(scenario, person);
        person.addPlan(source);
        person.setSelectedPlan(source);
        scenario.getPopulation().addPerson(person);

        var facilityFactory = scenario.getActivityFacilities().getFactory();
        Id<org.matsim.api.core.v01.network.Link> linkId = Id.createLinkId("inland");
        ActivityFacility originalFacility = facilityFactory.createActivityFacility(
                Id.create("original-work", ActivityFacility.class), new Coord(5_000.0, 0.0), linkId);
        originalFacility.addActivityOption(facilityFactory.createActivityOption("work"));
        ActivityFacility replacementFacility = facilityFactory.createActivityFacility(
                Id.create("replacement-work", ActivityFacility.class), new Coord(5_500.0, 0.0), linkId);
        replacementFacility.addActivityOption(facilityFactory.createActivityOption("work"));
        scenario.getActivityFacilities().addActivityFacility(originalFacility);
        scenario.getActivityFacilities().addActivityFacility(replacementFacility);

        Activity sourceInland = TripStructureUtils.getTrips(source).get(1).getDestinationActivity();
        sourceInland.setFacilityId(originalFacility.getId());
        sourceInland.setLinkId(linkId);

        TripRouter tripRouter = mock(TripRouter.class);
        when(tripRouter.calcRoute(anyString(), any(), any(), anyDouble(), any(), any()))
                .thenAnswer(invocation -> List.of(
                        scenario.getPopulation().getFactory().createLeg(invocation.getArgument(0))));
        Provider<TripRouter> provider = () -> tripRouter;
        BackgroundPlanRelocator relocator = new BackgroundPlanRelocator(
                scenario, provider, 0.20, 200.0, 5_000.0, 0.75);

        Plan proposal = relocator.propose(person, new Random(1L));

        assertNotNull(proposal);
        var proposalTrips = TripStructureUtils.getTrips(proposal);
        Activity proposalInland = proposalTrips.get(1).getDestinationActivity();
        assertEquals(replacementFacility.getId(), proposalInland.getFacilityId());
        assertSame(proposalInland, proposalTrips.get(2).getOriginActivity());
        assertEquals(0.0, proposalTrips.getFirst().getDestinationActivity().getCoord().getX(), 1.0e-9);
        assertEquals(0.0, proposalTrips.get(2).getDestinationActivity().getCoord().getX(), 1.0e-9);
        assertEquals(5_000.0, sourceInland.getCoord().getX(), 1.0e-9);
        verify(tripRouter, times(2)).calcRoute(anyString(), any(), any(), anyDouble(), any(), any());
    }

    private static Plan plan(Scenario scenario, Person person, Coord origin, Coord destination) {
        var factory = scenario.getPopulation().getFactory();
        Plan plan = factory.createPlan();
        plan.setPerson(person);
        Activity start = factory.createActivityFromCoord("origin", origin);
        start.setEndTime(0.0);
        plan.addActivity(start);
        plan.addLeg(factory.createLeg("truck"));
        plan.addActivity(factory.createActivityFromCoord("destination", destination));
        return plan;
    }

    private static Plan crossBorderPlan(Scenario scenario, Person person) {
        var factory = scenario.getPopulation().getFactory();
        Plan plan = factory.createPlan();
        plan.setPerson(person);
        addActivity(plan, factory, factory.createActivityFromCoord("outside", new Coord(-1_000.0, 0.0)), 0.0);
        addActivity(plan, factory, factory.createActivityFromCoord("border", new Coord(0.0, 0.0)), 100.0);
        addActivity(plan, factory, factory.createActivityFromCoord("work", new Coord(5_000.0, 0.0)), 200.0);
        addActivity(plan, factory, factory.createActivityFromCoord("border", new Coord(0.0, 0.0)), 300.0);
        plan.addLeg(factory.createLeg("car"));
        plan.addActivity(factory.createActivityFromCoord("outside", new Coord(-1_000.0, 0.0)));
        return plan;
    }

    private static void addActivity(Plan plan,
                                    PopulationFactory factory,
                                    Activity activity,
                                    double endTime) {
        activity.setEndTime(endTime);
        plan.addActivity(activity);
        plan.addLeg(factory.createLeg("car"));
    }
}
