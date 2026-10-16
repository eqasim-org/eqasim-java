package org.eqasim.core.components.travel_time;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup.ActivityParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;

import static org.junit.Assert.*;

public class TestFloatTravelTimeModule {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void floatStorageDefaultsOnAndRejectsInvalidArguments() throws Exception {
        assertTrue(FloatTravelTimeModule.isEnabled(command()));
        assertFalse(FloatTravelTimeModule.isEnabled(command("--float-travel-times", "false")));
        assertTrue(FloatTravelTimeModule.isEnabled(command("--float-travel-times", "true")));
        assertThrows(CommandLine.ConfigurationException.class,
                () -> FloatTravelTimeModule.isEnabled(command("--float-travel-times", "typo")));
    }

    @Test public void collectorsAreLazyAndSharedAcrossModesUnlessSeparateModesIsRequested() {
        var network = TestFloatTravelTimeCalculator.network();
        var config = ConfigUtils.createConfig();
        config.travelTimeCalculator().setSeparateModes(false);
        var events = EventsUtils.createEventsManager();
        var registry = new FloatTravelTimeModule.Registry(network, config, events);
        assertEquals(0, registry.size());
        assertSame(registry.get("car"), registry.get("truck"));
        assertSame(registry.get("car").getLinkTravelTimes(), registry.get("car_passenger").getLinkTravelTimes());
        assertEquals(1, registry.size());

        var separate = ConfigUtils.createConfig();
        separate.travelTimeCalculator().setSeparateModes(true);
        var separatedEvents = EventsUtils.createEventsManager();
        var separated = new FloatTravelTimeModule.Registry(network, separate, separatedEvents);
        var car = separated.get("car");
        var truck = separated.get("truck");
        assertNotSame(car, truck);
        assertSame(car, separated.get("car"));
        var id = Id.createLinkId("ab");
        for (String mode : List.of("car", "truck")) {
            var vehicle = Id.createVehicleId(mode);
            separatedEvents.processEvent(new VehicleEntersTrafficEvent(0, Id.createPersonId(mode), id, vehicle, mode, 0));
            separatedEvents.processEvent(new LinkEnterEvent(100, vehicle, id));
            separatedEvents.processEvent(new LinkLeaveEvent(mode.equals("car") ? 120 : 130, vehicle, id));
        }
        car.prepareForRouting(); truck.prepareForRouting();
        var link = network.getLinks().get(id);
        assertEquals(20, car.getLinkTravelTimes().getLinkTravelTime(link, 100, null, null), 0);
        assertEquals(30, truck.getLinkTravelTimes().getLinkTravelTime(link, 100, null, null), 0);
    }

    @Test public void controllerSharesObservedTimesAndPreservesLaterBikeOverrideAcrossIterations() throws Exception {
        var config = ConfigUtils.createConfig();
        config.controller().setOutputDirectory(temporary.newFolder("simulation").getAbsolutePath());
        config.controller().setLastIteration(1);
        config.global().setNumberOfThreads(2);
        config.qsim().setNumberOfThreads(1);
        config.qsim().setEndTime(3600);
        config.routing().setNetworkModes(List.of("car", "truck", "car_passenger", "bike"));
        config.routing().removeModeRoutingParams("bike");
        config.travelTimeCalculator().setSeparateModes(false);
        config.travelTimeCalculator().setAnalyzedModes(Set.of("car", "truck", "car_passenger"));
        config.replanning().addStrategySettings(new org.matsim.core.config.groups.ReplanningConfigGroup.StrategySettings()
                .setStrategyName("ReRoute").setWeight(1.0));
        config.scoring().addActivityParams(new ActivityParams("home").setTypicalDuration(3600));
        config.scoring().addActivityParams(new ActivityParams("work").setTypicalDuration(3600));
        var scenario = ScenarioUtils.createScenario(config);
        var network = TestFloatTravelTimeCalculator.network();
        var b = network.getNodes().get(Id.createNodeId("b"));
        var c = network.getFactory().createNode(Id.createNodeId("c"), new org.matsim.api.core.v01.Coord(200, 0));
        var d = network.getFactory().createNode(Id.createNodeId("d"), new org.matsim.api.core.v01.Coord(300, 0));
        network.addNode(c); network.addNode(d);
        for (var link : List.of(network.getFactory().createLink(Id.createLinkId("bc"), b, c),
                network.getFactory().createLink(Id.createLinkId("cb"), c, b),
                network.getFactory().createLink(Id.createLinkId("cd"), c, d),
                network.getFactory().createLink(Id.createLinkId("dc"), d, c))) {
            link.setLength(100); link.setFreespeed(10); link.setCapacity(1000); link.setNumberOfLanes(1);
            link.setAllowedModes(Set.of("car", "truck", "car_passenger", "bike")); network.addLink(link);
        }
        network.getNodes().values().forEach(scenario.getNetwork()::addNode);
        network.getLinks().values().forEach(scenario.getNetwork()::addLink);
        var factory = scenario.getPopulation().getFactory();
        for (int i = 0; i < 4; i++) {
            var person = factory.createPerson(Id.createPersonId(i));
            var plan = factory.createPlan();
            var home = factory.createActivityFromLinkId("home", Id.createLinkId("ab"));
            home.setEndTime(100 + i * 10);
            plan.addActivity(home);
            var leg = factory.createLeg("car");
            leg.setDepartureTime(100 + i * 10);
            leg.setTravelTime(0);
            org.matsim.core.router.TripStructureUtils.setRoutingMode(leg, "car");
            plan.addLeg(leg);
            plan.addActivity(factory.createActivityFromLinkId("work", Id.createLinkId("cd")));
            person.addPlan(plan); scenario.getPopulation().addPerson(person);
        }
        TravelTime bike = (link, time, person, vehicle) -> 123;
        var controller = new Controler(scenario);
        controller.addOverridingModule(new FloatTravelTimeModule());
        controller.addOverridingModule(new AbstractModule() {
            @Override public void install() {
                bind(org.eqasim.core.components.flow.FlowUtils.class)
                        .toInstance(new org.eqasim.core.components.flow.FlowUtils(scenario));
                addTravelTimeBinding("bike").toInstance(bike);
                for (String mode : config.routing().getNetworkModes()) addTravelDisutilityFactoryBinding(mode)
                        .toInstance(new org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutilityFactory());
            }
        });
        controller.run();
        var times = controller.getInjector().getInstance(Key.get(new TypeLiteral<Map<String, TravelTime>>() { }));
        assertSame(times.get("car"), times.get("truck"));
        assertSame(times.get("car"), times.get("car_passenger"));
        assertSame(bike, times.get("bike"));
        var registry = controller.getInjector().getInstance(FloatTravelTimeModule.Registry.class);
        assertEquals(1, registry.size());
        var stats = registry.get("car").getStorageStatistics();
        assertTrue(stats.preparedForRouting());
        assertTrue(stats.linkRows() > 0);
        assertEquals(0, stats.observationCountBytes());
    }

    private static CommandLine command(String... args) throws Exception {
        return new CommandLine.Builder(args).allowOptions(FloatTravelTimeModule.OPTION).build();
    }
}
