package org.matsim.core.mobsim.qsim.qnetsimengine;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;
import static org.junit.Assert.*;

public class TestFastQNetsimEngine {
    @Test(timeout = 120_000) public void holeTrafficModelsAndCapacityVariantsMatchNativeScheduling() {
        for (var traffic : List.of(QSimConfigGroup.TrafficDynamics.withHoles,
                QSimConfigGroup.TrafficDynamics.kinematicWaves)) {
            for (var dynamics : QSimConfigGroup.LinkDynamics.values()) {
                for (boolean fastCapacity : new boolean[] {false, true}) {
                    assertEquals(traffic + " / " + dynamics + " / fastCapacity=" + fastCapacity,
                            run(4, dynamics, false, traffic, fastCapacity),
                            run(4, dynamics, true, traffic, fastCapacity));
                }
            }
        }
    }

    @Test(timeout = 120_000) public void manyWorkersAndActivationFallbackPreserveEvents() {
        var expected = run(4, QSimConfigGroup.LinkDynamics.SeepageQ, false);
        String previous = System.getProperty("eqasim.qsim.arrayActivations");
        try {
            for (int threads : new int[] {24, 48}) {
                for (String setting : List.of("true", "false")) {
                    System.setProperty("eqasim.qsim.arrayActivations", setting);
                    assertEquals(expected, run(threads, QSimConfigGroup.LinkDynamics.SeepageQ, true));
                }
            }
        } finally {
            if (previous == null) System.clearProperty("eqasim.qsim.arrayActivations");
            else System.setProperty("eqasim.qsim.arrayActivations", previous);
        }
    }

    @Test(timeout = 120_000) public void congestedMergesSplitsAndCrossPartitionTripsMatchNative() {
        for (int threads : new int[] {1, 4}) {
            for (var dynamics : QSimConfigGroup.LinkDynamics.values()) {
                assertEquals("threads=" + threads + " dynamics=" + dynamics,
                        run(threads, dynamics, false), run(threads, dynamics, true));
            }
        }
    }

    static List<String> run(int threads, QSimConfigGroup.LinkDynamics dynamics, boolean fast) {
        return run(threads, dynamics, fast, null, null);
    }

    static List<String> run(int threads, QSimConfigGroup.LinkDynamics dynamics, boolean fast,
            QSimConfigGroup.TrafficDynamics traffic, Boolean fastCapacity) {
        return run(threads, dynamics, fast, traffic, fastCapacity, null);
    }

    static List<String> run(int threads, QSimConfigGroup.LinkDynamics dynamics, boolean fast,
            QSimConfigGroup.TrafficDynamics traffic, Boolean fastCapacity,
            org.matsim.core.mobsim.qsim.qnetsimengine.flow_efficiency.FlowEfficiencyCalculator efficiency) {
        MatsimRandom.reset(42);
        var config = ConfigUtils.createConfig();
        config.qsim().setNumberOfThreads(threads);
        config.qsim().setMainModes(Set.of("car", "bike"));
        config.qsim().setLinkDynamics(dynamics);
        if (traffic != null) config.qsim().setTrafficDynamics(traffic);
        if (fastCapacity != null) config.qsim().setUsingFastCapacityUpdate(fastCapacity);
        config.qsim().setSeepModes(Set.of("bike"));
        config.qsim().setRestrictingSeepage(true);
        config.qsim().setSeepModeStorageFree(true);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
        config.qsim().setStartTime(0); config.qsim().setEndTime(5000);
        config.qsim().setStuckTime(20_000);
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var factory = network.getFactory();
        for (int c = 0; c < 8; c++) {
            for (int n = 0; n < 4; n++) {
                var node = factory.createNode(Id.createNodeId(c + "-" + n),
                        new Coord(c * 1000 + (n > 1 ? 300 : 0), n % 2 * 100));
                network.addNode(node);
            }
        }
        for (int c = 0; c < 8; c++) {
            addLink(scenario, "a" + c, c + "-0", c + "-2", 100, 1200);
            addLink(scenario, "b" + c, c + "-1", c + "-2", 100, 600);
            addLink(scenario, "road" + c, c + "-2", c + "-3", 200, c % 2 == 0 ? 600 : 1200);
            if (c < 7) {
                addLink(scenario, "joinA" + c, c + "-3", (c + 1) + "-0", 100, 1200);
                addLink(scenario, "joinB" + c, c + "-3", (c + 1) + "-1", 100, 1200);
            }
        }
        var population = scenario.getPopulation().getFactory();
        for (String mode : List.of("car", "bike")) {
            var type = VehicleUtils.createVehicleType(Id.create(mode, org.matsim.vehicles.VehicleType.class));
            type.setNetworkMode(mode); type.setMaximumVelocity(mode.equals("bike") ? 5 : 20);
            type.setPcuEquivalents(mode.equals("bike") ? 0.25 : 1);
            type.setFlowEfficiencyFactor(mode.equals("bike") ? 0.8 : 1.2);
            scenario.getVehicles().addVehicleType(type);
            for (int i = 0; i < 100; i++) {
                int origin = i < 80 ? 0 : i % 7;
                var id = Id.createPersonId(mode + i);
                var vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), type);
                scenario.getVehicles().addVehicle(vehicle);
                var person = population.createPerson(id);
                var plan = population.createPlan();
                var start = Id.createLinkId((i % 2 == 0 ? "a" : "b") + origin);
                var home = population.createActivityFromLinkId("home", start);
                home.setEndTime(i % 20); plan.addActivity(home);
                var leg = population.createLeg(mode);
                List<Id<Link>> middle = new ArrayList<>();
                for (int c = origin; c < 7; c++) {
                    middle.add(Id.createLinkId("road" + c));
                    boolean a = (c + i) % 2 == 0;
                    middle.add(Id.createLinkId((a ? "joinA" : "joinB") + c));
                    middle.add(Id.createLinkId((a ? "a" : "b") + (c + 1)));
                }
                var route = RouteUtils.createLinkNetworkRouteImpl(start, middle, Id.createLinkId("road7"));
                route.setVehicleId(vehicle.getId()); leg.setRoute(route); plan.addLeg(leg);
                plan.addActivity(population.createActivityFromLinkId("work", Id.createLinkId("road7")));
                person.addPlan(plan); scenario.getPopulation().addPerson(person);
            }
        }
        var events = EventsUtils.createEventsManager();
        List<String> observed = Collections.synchronizedList(new ArrayList<>());
        events.addHandler((BasicEventHandler) event -> observed.add(new TreeMap<>(event.getAttributes()).toString()));
        var qsim = new QSimBuilder(config).useDefaults().addOverridingQSimModule(new AbstractQSimModule() {
            @Override protected void configureQSim() {
                if (fast) bind(QNetsimEngineI.class).to(FastQNetsimEngine.class).in(Singleton.class);
                if (efficiency != null) bind(QNetworkFactory.class).toProvider(() -> {
                    var networkFactory = new ConfigurableQNetworkFactory(events, scenario);
                    networkFactory.setFlowEfficiencyCalculator(efficiency);
                    return networkFactory;
                }).in(Singleton.class);
            }
        }).build(scenario, events);
        qsim.run();
        assertEquals(200, observed.stream().filter(event -> event.contains("type=arrival")).count());
        assertFalse(observed.stream().anyMatch(event -> event.contains("type=stuck")));
        Collections.sort(observed);
        return observed;
    }

    private static void addLink(org.matsim.api.core.v01.Scenario scenario, String id,
            String from, String to, double length, double capacity) {
        var network = scenario.getNetwork();
        var link = network.getFactory().createLink(Id.createLinkId(id),
                network.getNodes().get(Id.createNodeId(from)), network.getNodes().get(Id.createNodeId(to)));
        link.setLength(length); link.setFreespeed(20); link.setCapacity(capacity);
        link.setNumberOfLanes(1); link.setAllowedModes(Set.of("car", "bike"));
        network.addLink(link);
    }
}
