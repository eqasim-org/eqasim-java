package org.eqasim.switzerland.ch_cmdp.dsim;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.components.traffic.CrossingPenalty;
import org.eqasim.core.components.transit.departure.DefaultDepartureFinder;
import org.eqasim.core.components.transit.departure.DepartureFinder;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.simulation.dsim.EqasimDsimModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.routes.DefaultTransitPassengerRoute;

public class TestDsimIntegration {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void optionAndAdaptation() throws Exception {
        assertFalse(DsimIntegration.isEnabled(new CommandLine.Builder(new String[] {}).build()));
        assertNull(DsimIntegration.fromCommand(new CommandLine.Builder(new String[] {}).build()));
        assertNotNull(DsimIntegration.fromCommand(new CommandLine.Builder(new String[] {"--use-dsim", "true"})
                .allowOptions(DsimIntegration.OPTION, DsimIntegration.SIMULATE_BIKES_OPTION).build()));
        assertThrows(CommandLine.ConfigurationException.class, () -> DsimIntegration.fromCommand(
                new CommandLine.Builder(new String[] {"--use-dsim", "typo"})
                        .allowOptions(DsimIntegration.OPTION).build()));
        assertTrue(DsimIntegration.isEnabled(new CommandLine.Builder(new String[] {"--use-dsim", "true"})
                .allowOptions(DsimIntegration.OPTION).build()));
        assertThrows(CommandLine.ConfigurationException.class, () -> DsimIntegration.fromCommand(
                new CommandLine.Builder(new String[] {"--dsim-simulate-bikes", "true"})
                        .allowOptions(DsimIntegration.OPTION, DsimIntegration.SIMULATE_BIKES_OPTION).build()));
        assertNotNull(DsimIntegration.fromCommand(new CommandLine.Builder(new String[] {
                "--use-dsim", "true", "--dsim-simulate-bikes", "true"})
                .allowOptions(DsimIntegration.OPTION, DsimIntegration.SIMULATE_BIKES_OPTION).build()));
        var config = ConfigUtils.createConfig(new EqasimConfigGroup());
        config.qsim().setMainModes(Set.of("car", "bike", "truck"));
        config.routing().setNetworkModes(Set.of("car", "bike", "truck"));
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);
        config.qsim().setFlowCapFactor(0.1);
        config.qsim().setStorageCapFactor(0.2);
        config.qsim().setEndTime(108000);
        config.dsim().setThreads(4);
        new DsimIntegration2026().adaptConfig(config);
        assertEquals(Set.of("car", "truck"), config.dsim().getNetworkModes());
        assertEquals(4, config.dsim().getThreads());
        assertEquals(108000, config.dsim().getEndTime(), 0);
        assertEquals(0.2, config.qsim().getStorageCapFactor(), 0);
        assertEquals(4, config.routing().getTeleportedModeParams().get("bike").getTeleportedModeSpeed(), 0);
        assertEquals(1.4, config.routing().getTeleportedModeParams().get("bike").getBeelineDistanceFactor(), 0);
        assertFalse(config.transit().isUsingTransitInMobsim());
    }

    @Test public void roadsBikesAndScheduledPtAcrossPartitionsAndIterations() throws Exception {
        for (int threads : new int[] {1, 2}) {
            var scenario = scenario(threads);
            var events = new ConcurrentLinkedQueue<Event>();
            var controller = new Controler(scenario);
            controller.addOverridingModule(new AbstractModule() {
                @Override public void install() {
                    bind(org.eqasim.core.components.flow.FlowUtils.class).toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
                    bind(CrossingPenalty.class).toInstance((link, now, vehicle) -> 7.0);
                    bind(DepartureFinder.class).to(DefaultDepartureFinder.class);
                    addEventHandlerBinding().toInstance(new BasicEventHandler() {
                        @Override public void handleEvent(Event event) { events.add(event); }
                    });
                }
            });
            controller.addOverridingModule(new EqasimDsimModule());
            controller.addOverridingQSimModule(EqasimDsimModule.engineModule());
            controller.run();
            for (var person : scenario.getPopulation().getPersons().values()) {
                assertNotNull(person.getSelectedPlan().getScore());
                assertTrue(Double.isFinite(person.getSelectedPlan().getScore()));
            }
            assertFalse(events.stream().anyMatch(e -> e instanceof PersonStuckEvent));
            for (String id : List.of("car", "bike", "early", "missed")) {
                assertEquals("Both iterations must finish " + id, 2, events.stream()
                        .filter(e -> e instanceof PersonArrivalEvent arrival && arrival.getPersonId().toString().equals(id)).count());
            }
            var pt = events.stream().filter(PublicTransitEvent.class::isInstance).map(PublicTransitEvent.class::cast).toList();
            assertEquals(4, pt.size());
            for (var event : pt) {
                assertEquals(event.getPersonId().toString().equals("early") ? 60 : 160, event.getTime(), 0);
                assertEquals(event.getPersonId().toString().equals("early") ? 10 : 110, event.getVehicleDepartureTime(), 0);
            }
        }
    }

    @Test public void bicycleUsesIsolatedNetworkLayerWhenExplicitlyRequested() throws Exception {
        var scenario = scenario(2, true);
        assertEquals(0.0, scenario.getVehicles().getVehicleTypes().get(
                Id.create(TransportMode.bike, org.matsim.vehicles.VehicleType.class)).getPcuEquivalents(), 0.0);
        var events = new ConcurrentLinkedQueue<Event>();
        var controller = new Controler(scenario);
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(org.eqasim.core.components.flow.FlowUtils.class)
                        .toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
                bind(CrossingPenalty.class).toInstance((link, now, vehicle) -> 7.0);
                bind(DepartureFinder.class).to(DefaultDepartureFinder.class);
                addEventHandlerBinding().toInstance(new BasicEventHandler() {
                    @Override public void handleEvent(Event event) { events.add(event); }
                });
            }
        });
        controller.addOverridingModule(new EqasimDsimModule());
        controller.addOverridingQSimModule(EqasimDsimModule.engineModule());
        controller.run();
        assertEquals(2, events.stream().filter(event -> event instanceof PersonArrivalEvent arrival
                && arrival.getPersonId().toString().equals("bike")
                && arrival.getLegMode().equals(TransportMode.bike)).count());
        assertTrue(events.stream().anyMatch(event -> event instanceof LinkEnterEvent enter
                && enter.getVehicleId().toString().contains("bike")
                && DsimBikeNetworkAdapter.isBikeLink(enter.getLinkId())));
        assertTrue(scenario.getNetwork().getLinks().values().stream()
                .filter(link -> !DsimBikeNetworkAdapter.isBikeLink(link.getId()))
                .noneMatch(link -> link.getAllowedModes().contains(TransportMode.bike)));
    }

    private Scenario scenario(int threads) throws Exception {
        return scenario(threads, false);
    }

    private Scenario scenario(int threads, boolean networkBikes) throws Exception {
        var config = ConfigUtils.createConfig(new EqasimConfigGroup());
        config.controller().setOutputDirectory(temp.newFolder("run" + threads).toString());
        config.controller().setLastIteration(1);
        config.controller().setWriteEventsInterval(0);
        config.controller().setCreateGraphs(false);
        config.replanning().addStrategySettings(new org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings()
                .setStrategyName("KeepLastSelected").setWeight(1.0));
        config.qsim().setNumberOfThreads(threads);
        config.qsim().setEndTime(300);
        config.qsim().setTrafficDynamics(QSimConfigGroup.TrafficDynamics.queue);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.defaultVehicle);
        config.qsim().setMainModes(Set.of(TransportMode.car, TransportMode.bike));
        config.routing().setNetworkModes(Set.of(TransportMode.car, TransportMode.bike));
        config.transit().setUseTransit(true);
        config.routing().getOrCreateModeRoutingParams("walk").setTeleportedModeSpeed(1.3);
        config.routing().getOrCreateModeRoutingParams("walk").setBeelineDistanceFactor(1.3);
        config.scoring().addActivityParams(new org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams("home").setTypicalDuration(3600));
        var adapter = new DsimIntegration2026(networkBikes);
        adapter.adaptConfig(config);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        for (int i = 0; i < 4; i++) NetworkUtils.createAndAddNode(network, Id.createNodeId(i), new Coord(i * 100, 0));
        for (int i = 0; i < 3; i++) {
            NetworkUtils.createAndAddLink(network, Id.createLinkId("l" + i), network.getNodes().get(Id.createNodeId(i)),
                    network.getNodes().get(Id.createNodeId(i + 1)), 100, 10, 3600, 1);
            NetworkUtils.createAndAddLink(network, Id.createLinkId("r" + i), network.getNodes().get(Id.createNodeId(i + 1)),
                    network.getNodes().get(Id.createNodeId(i)), 100, 10, 3600, 1);
        }
        network.getLinks().values().forEach(link -> link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike)));
        var schedule = scenario.getTransitSchedule();
        var sf = schedule.getFactory();
        var a = sf.createTransitStopFacility(Id.create("a", org.matsim.pt.transitSchedule.api.TransitStopFacility.class), new Coord(100, 0), false);
        var b = sf.createTransitStopFacility(Id.create("b", org.matsim.pt.transitSchedule.api.TransitStopFacility.class), new Coord(300, 0), false);
        a.setLinkId(Id.createLinkId("l0")); b.setLinkId(Id.createLinkId("l2"));
        schedule.addStopFacility(a); schedule.addStopFacility(b);
        var route = RouteUtils.createLinkNetworkRouteImpl(a.getLinkId(), b.getLinkId());
        route.setLinkIds(a.getLinkId(), List.of(Id.createLinkId("l1")), b.getLinkId());
        var line = sf.createTransitLine(Id.create("line", org.matsim.pt.transitSchedule.api.TransitLine.class));
        var service = sf.createTransitRoute(Id.create("service", org.matsim.pt.transitSchedule.api.TransitRoute.class), route,
                List.of(sf.createTransitRouteStop(a, 0, 0), sf.createTransitRouteStop(b, 50, 50)), "bus");
        for (int i = 0; i < 2; i++) service.addDeparture(sf.createDeparture(Id.create("d" + i, org.matsim.pt.transitSchedule.api.Departure.class), 10 + i * 100));
        line.addRoute(service); schedule.addTransitLine(line);
        var pf = scenario.getPopulation().getFactory();
        for (String id : List.of("car", "bike", "early", "missed")) {
            var person = pf.createPerson(Id.createPersonId(id));
            var plan = pf.createPlan();
            var start = pf.createActivityFromCoord("home", new Coord(100, 0)); start.setLinkId(a.getLinkId());
            double departure = id.equals("missed") ? 15 : 5;
            start.setEndTime(departure); plan.addActivity(start);
            String mode = id.equals("early") || id.equals("missed") ? "pt" : id;
            var leg = pf.createLeg(mode); TripStructureUtils.setRoutingMode(leg, mode);
            leg.setDepartureTime(departure);
            if (mode.equals("car")) { leg.setRoute(route.clone()); leg.getRoute().setDistance(200); leg.setTravelTime(20); }
            if (mode.equals("pt")) {
                var pt = new DefaultTransitPassengerRoute(a.getLinkId(), b.getLinkId(), a.getId(), b.getId(), line.getId(), service.getId());
                pt.setBoardingTime(10); pt.setTravelTime(55); pt.setDistance(200); leg.setRoute(pt); leg.setTravelTime(55);
            }
            plan.addLeg(leg);
            var end = pf.createActivityFromCoord("home", new Coord(300, 0)); end.setLinkId(b.getLinkId()); plan.addActivity(end);
            person.addPlan(plan); scenario.getPopulation().addPerson(person);
        }
        adapter.adaptScenario(scenario);
        return scenario;
    }
}
