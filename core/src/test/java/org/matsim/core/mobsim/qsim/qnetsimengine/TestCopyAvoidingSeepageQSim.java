package org.matsim.core.mobsim.qsim.qnetsimengine;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.eqasim.core.components.traffic.EqasimLinkSpeedCalculator;
import org.eqasim.core.components.traffic.EqasimTrafficQSimModule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.VehicleUtils;

public class TestCopyAvoidingSeepageQSim {
    @Test public void mixedCongestedTrafficMatchesNativeWithOneAndFourWorkers() {
        for (int threads : List.of(1, 4)) for (boolean restricted : List.of(false, true)) {
            assertEquals("threads=" + threads + " restricted=" + restricted,
                    run(threads, restricted, false), run(threads, restricted, true));
        }
    }

    private static List<String> run(int threads, boolean restricted, boolean optimized) {
        MatsimRandom.reset(42);
        var config = ConfigUtils.createConfig();
        config.qsim().setNumberOfThreads(threads);
        config.qsim().setMainModes(Set.of("car", "bike"));
        config.qsim().setLinkDynamics(QSimConfigGroup.LinkDynamics.SeepageQ);
        config.qsim().setSeepModes(Set.of("bike"));
        config.qsim().setRestrictingSeepage(restricted);
        config.qsim().setSeepModeStorageFree(true);
        config.qsim().setVehiclesSource(QSimConfigGroup.VehiclesSource.fromVehiclesData);
        config.qsim().setStartTime(0); config.qsim().setEndTime(5000);
        config.qsim().setStuckTime(10_000);
        config.qsim().setNodeOffset(1); // Exercise the concrete lane snapshot-data cast.
        var scenario = ScenarioUtils.createScenario(config);
        var network = scenario.getNetwork();
        var factory = network.getFactory();
        for (int i = 0; i < 5; i++) network.addNode(factory.createNode(Id.createNodeId(i), new Coord(i * 1000, 0)));
        for (int i = 0; i < 4; i++) {
            var link = factory.createLink(Id.createLinkId(i), network.getNodes().get(Id.createNodeId(i)),
                    network.getNodes().get(Id.createNodeId(i + 1)));
            link.setLength(i == 1 ? 1000 : 100); link.setFreespeed(20);
            link.setCapacity(600); link.setNumberOfLanes(1); link.setAllowedModes(Set.of("car", "bike"));
            network.addLink(link);
        }
        var population = scenario.getPopulation().getFactory();
        for (String mode : List.of("car", "bike")) {
            var type = VehicleUtils.createVehicleType(Id.create(mode, org.matsim.vehicles.VehicleType.class));
            type.setNetworkMode(mode); type.setMaximumVelocity(mode.equals("bike") ? 5 : 20);
            type.setPcuEquivalents(mode.equals("bike") ? 0.25 : 1);
            scenario.getVehicles().addVehicleType(type);
            for (int i = 0; i < 80; i++) {
                var id = Id.createPersonId(mode + i);
                var vehicle = VehicleUtils.createVehicle(Id.createVehicleId(id), type);
                scenario.getVehicles().addVehicle(vehicle);
                var person = population.createPerson(id);
                VehicleUtils.insertVehicleIdsIntoAttributes(person, java.util.Map.of(mode, vehicle.getId()));
                var plan = population.createPlan();
                var home = population.createActivityFromLinkId("home", Id.createLinkId(0));
                home.setEndTime(i % 10); plan.addActivity(home);
                var leg = population.createLeg(mode);
                var route = RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId(0),
                        List.of(Id.createLinkId(1), Id.createLinkId(2)), Id.createLinkId(3));
                route.setVehicleId(vehicle.getId()); leg.setRoute(route); plan.addLeg(leg);
                plan.addActivity(population.createActivityFromLinkId("work", Id.createLinkId(3)));
                person.addPlan(plan); scenario.getPopulation().addPerson(person);
            }
        }
        var events = EventsUtils.createEventsManager();
        List<String> observed = Collections.synchronizedList(new ArrayList<>());
        events.addHandler((BasicEventHandler) event -> observed.add(new TreeMap<>(event.getAttributes()).toString()));
        var builder = new QSimBuilder(config).useDefaults()
                .addOverridingModule(new AbstractModule() {
                    @Override public void install() {
                        bind(EqasimLinkSpeedCalculator.class).toInstance(new EqasimLinkSpeedCalculator() {
                            @Override public double getMaximumCarVelocity(QVehicle vehicle,
                                    org.matsim.api.core.v01.network.Link link, double time) { return 20; }
                            @Override public double getMaximumBikeVelocity(QVehicle vehicle,
                                    org.matsim.api.core.v01.network.Link link, double time) { return 5; }
                        });
                    }
                }).addOverridingQSimModule(new EqasimTrafficQSimModule());
        if (!optimized) builder.addOverridingQSimModule(new AbstractQSimModule() {
            @Override protected void configureQSim() { bind(QNetworkFactory.class).to(NativeSeepageReferenceFactory.class); }
        });
        var qsim = builder.build(scenario, events);
        assertEquals(optimized ? DefaultQNetworkFactory.class : NativeSeepageReferenceFactory.class,
                qsim.getChildInjector().getInstance(QNetworkFactory.class).getClass());
        int[] snapshots = { 0 };
        qsim.addQueueSimulationListeners((org.matsim.core.mobsim.framework.listeners.MobsimBeforeSimStepListener) event -> {
            if (event.getSimulationTime() % 20 == 0) {
                for (var link : qsim.getNetsimNetwork().getNetsimLinks().values()) {
                    snapshots[0] += link.getVisData().addAgentSnapshotInfo(new ArrayList<>()).size();
                }
            }
        });
        qsim.run();
        assertTrue(snapshots[0] > 0);
        assertEquals(160, observed.stream().filter(event -> event.contains("type=arrival")).count());
        assertFalse(observed.stream().anyMatch(event -> event.contains("type=stuck")));
        Collections.sort(observed); // Cross-worker event delivery order may differ; event content must match.
        return observed;
    }
}
