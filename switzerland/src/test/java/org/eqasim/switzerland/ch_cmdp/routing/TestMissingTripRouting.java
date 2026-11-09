package org.eqasim.switzerland.ch_cmdp.routing;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.*;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.ReplanningConfigGroup;
import org.matsim.core.controler.*;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.*;
import org.matsim.core.router.*;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.timing.TimeInterpretation;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

public class TestMissingTripRouting {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private Scenario scenario(boolean networkBike) {
        var config = ConfigUtils.createConfig();
        config.routing().setNetworkModes(networkBike ? Set.of("car", "bike") : Set.of("car"));
        if (networkBike) config.routing().removeTeleportedModeParams("bike");
        config.qsim().setMainModes(networkBike ? Set.of("car", "bike") : Set.of("car"));
        var scenario = ScenarioUtils.createScenario(config); var network = scenario.getNetwork();
        var a = NetworkUtils.createAndAddNode(network, Id.createNodeId("a"), new Coord(0,0));
        var b = NetworkUtils.createAndAddNode(network, Id.createNodeId("b"), new Coord(100,0));
        var road = NetworkUtils.createAndAddLink(network, Id.createLinkId("road"), a,b,100,10,1800,1);
        var back = NetworkUtils.createAndAddLink(network, Id.createLinkId("back"), b,a,100,10,1800,1);
        road.setAllowedModes(Set.of("car", "bike")); back.setAllowedModes(Set.of("car", "bike"));
        var pf = scenario.getPopulation().getFactory(); var person = pf.createPerson(Id.createPersonId("person"));
        var plan = pf.createPlan(); person.addPlan(plan); scenario.getPopulation().addPerson(person);
        var home = pf.createActivityFromLinkId("home", road.getId()); home.setEndTime(1000); plan.addActivity(home);
        plan.addLeg(leg(scenario,"car",true));
        var middle = pf.createActivityFromLinkId("home", road.getId()); middle.setMaximumDuration(50); plan.addActivity(middle);
        plan.addLeg(leg(scenario,"bike",false));
        plan.addActivity(pf.createActivityFromLinkId("home", road.getId()));
        return scenario;
    }

    private Leg leg(Scenario scenario, String mode, boolean network) {
        var leg = scenario.getPopulation().getFactory().createLeg(mode); TripStructureUtils.setRoutingMode(leg, mode);
        var id = Id.createLinkId("road");
        var route = network ? RouteUtils.createLinkNetworkRouteImpl(id,id) : RouteUtils.createGenericRouteImpl(id,id);
        route.setTravelTime(100); route.setDistance(100); leg.setRoute(route); leg.setTravelTime(100); leg.setDepartureTime(1000);
        return leg;
    }

    @Test public void repairsOnlyIncompatibleBikeAndUsesPrecedingTripTime() {
        var scenario = scenario(true); var plan = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
        var car = (Leg) plan.getPlanElements().get(1); var originalRoute = car.getRoute();
        var router = mock(TripRouter.class);
        when(router.calcRoute(eq("bike"), any(), any(), anyDouble(), any(), any())).thenAnswer(call -> List.of(leg(scenario,"bike",true)));
        var worker = new MissingTripRouting.Worker(scenario, router, TimeInterpretation.create(scenario.getConfig()));
        assertEquals(1, worker.run(plan, Set.of()));
        verify(router, times(1)).calcRoute(eq("bike"), any(), any(), eq(1150.0), any(), any());
        verifyNoMoreInteractions(router);
        assertSame(car, plan.getPlanElements().get(1)); assertSame(originalRoute, car.getRoute());
        assertTrue(((Leg)plan.getPlanElements().get(3)).getRoute() instanceof NetworkRoute);
        assertEquals(0, worker.run(plan, Set.of()));
    }

    @Test public void teleportedBikesAndCompleteTripsDoNotTriggerAnyRouting() {
        var scenario = scenario(false); var created = new AtomicInteger();
        MissingTripRouting.run(scenario, 2, Set.of(), () -> { created.incrementAndGet(); throw new AssertionError("Router should not be built"); });
        assertEquals(0, created.get());
    }

    @Test public void missingNullRouteAndModeFilterAreRespected() {
        var scenario = scenario(true); var plan = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
        var bike = (Leg)plan.getPlanElements().get(3); bike.setRoute(null);
        var router = mock(TripRouter.class);
        var worker = new MissingTripRouting.Worker(scenario, router, TimeInterpretation.create(scenario.getConfig()));
        assertEquals(0, worker.run(plan, Set.of("car"))); verifyNoInteractions(router);
        when(router.calcRoute(eq("bike"), any(), any(), anyDouble(), any(), any())).thenAnswer(call -> List.of(leg(scenario,"bike",true)));
        assertEquals(1, worker.run(plan, Set.of("bike")));
    }

    @Test public void detectsRoutesOnTheWrongBikeLayer() {
        var scenario = scenario(true); var plan = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
        plan.getPlanElements().set(3, leg(scenario,"bike",true));
        assertFalse(MissingTripRouting.needsRouting(TripStructureUtils.getTrips(plan).get(1), scenario));
        scenario.getNetwork().getLinks().get(Id.createLinkId("road")).setAllowedModes(Set.of("car"));
        assertTrue(MissingTripRouting.needsRouting(TripStructureUtils.getTrips(plan).get(1), scenario));
    }

    @Test public void usableLegTimeDoesNotRequireAnotherRouteLevelTime() {
        var scenario = scenario(false);
        var plan = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
        ((Leg)plan.getPlanElements().get(1)).getRoute().setTravelTimeUndefined();
        var router = mock(TripRouter.class);
        var worker = new MissingTripRouting.Worker(scenario, router, TimeInterpretation.create(scenario.getConfig()));
        assertEquals(0, worker.run(plan, Set.of())); verifyNoInteractions(router);
    }

    @Test public void startupDoesNotRerouteTheCompleteCarTrip() throws Exception { startup(true); }
    @Test public void startupKeepsAlreadyTeleportedBikeWithoutNetworkRouting() throws Exception { startup(false); }

    @Test public void standaloneWritesCompletePopulationWithoutBuildingRouter() throws Exception {
        var scenario = scenario(false); var folder = temp.newFolder().toPath();
        var network = folder.resolve("network.xml"); var plans = folder.resolve("plans.xml");
        var config = folder.resolve("config.xml"); var output = folder.resolve("output.xml.gz");
        new org.matsim.core.network.io.NetworkWriter(scenario.getNetwork()).write(network.toString());
        new org.matsim.core.population.io.PopulationWriter(scenario.getPopulation()).write(plans.toString());
        var inputConfig = ConfigUtils.createConfig();
        inputConfig.network().setInputFile(network.toString());
        inputConfig.plans().setInputFile(plans.toString());
        new ConfigWriter(inputConfig).write(config.toString());
        org.eqasim.switzerland.ch_cmdp.routing.RunMissingPopulationRouting.main(new String[]{
                "--config-path", config.toString(), "--output-path", output.toString(), "--threads", "2"});
        var saved = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        new org.matsim.core.population.io.PopulationReader(saved).readFile(output.toString());
        var elements = saved.getPopulation().getPersons().get(Id.createPersonId("person")).getSelectedPlan().getPlanElements();
        assertTrue(((Leg)elements.get(1)).getRoute() instanceof NetworkRoute);
        assertFalse(((Leg)elements.get(3)).getRoute() instanceof NetworkRoute);
        assertEquals(100.0, ((Leg)elements.get(1)).getTravelTime().seconds(), 0);
    }

    private void startup(boolean networkBike) throws Exception {
        var scenario = scenario(networkBike); var config = scenario.getConfig();
        config.controller().setOutputDirectory(temp.newFolder().toString());
        config.controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controller().setLastIteration(0); config.controller().setWriteEventsInterval(0);
        config.controller().setCreateGraphs(false); config.scoring().setWriteExperiencedPlans(false);
        config.global().setNumberOfThreads(2);
        config.replanning().addStrategySettings(new ReplanningConfigGroup.StrategySettings().setStrategyName("KeepLastSelected").setWeight(1));
        var plan = scenario.getPopulation().getPersons().values().iterator().next().getSelectedPlan();
        var car = (Leg) plan.getPlanElements().get(1); var carRoute = car.getRoute();
        var carCalls = new AtomicInteger(); var bikeCalls = new AtomicInteger();
        var controller = new Controler(scenario);
        controller.addOverridingModule(new MissingTripRoutingModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                addRoutingModuleBinding("car").toInstance(request -> { carCalls.incrementAndGet(); return List.of(leg(scenario,"car",true)); });
                addRoutingModuleBinding("bike").toInstance(request -> { bikeCalls.incrementAndGet(); return List.of(leg(scenario,"bike",networkBike)); });
                bind(Mobsim.class).toInstance(() -> {
                    assertSame(car, plan.getPlanElements().get(1)); assertSame(carRoute, car.getRoute());
                    assertEquals(networkBike, ((Leg)plan.getPlanElements().get(3)).getRoute() instanceof NetworkRoute);
                });
            }
        });
        controller.run(); assertEquals(0, carCalls.get()); assertEquals(networkBike ? 1 : 0, bikeCalls.get());
    }
}
