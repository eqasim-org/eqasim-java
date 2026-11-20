package org.eqasim.switzerland.ch_cmdp.utils.pt;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.switzerland.ch_cmdp.PTPassengerCountsModule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.*;

public class TestPTPassengerCountingHandler {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    @Test public void timetableAndPhysicalVehicleEventsProduceTheSameCounts() throws Exception {
        var scenario = scenario(false);
        var scheduled = handler(scenario);
        var events = EventsUtils.createEventsManager();
        events.addHandler(scheduled);
        events.initProcessing();
        events.processEvent(ride("p1", 1000, 910, "a", "c", "d0"));
        events.processEvent(ride("p2", 1000, 910, "a", "c", "d0"));
        events.finishProcessing();

        var physical = handler(scenario);
        var vehicle = Id.createVehicleId("shared");
        physical.handleEvent(new VehicleArrivesAtFacilityEvent(890, vehicle, stopId("a"), 0));
        for (String person : List.of("p1", "p2"))
            physical.handleEvent(new PersonEntersVehicleEvent(900, Id.createPersonId(person), vehicle));
        physical.handleEvent(new VehicleDepartsAtFacilityEvent(910, vehicle, stopId("a"), 0));
        physical.handleEvent(new VehicleArrivesAtFacilityEvent(1000, vehicle, stopId("c"), 0));
        for (String person : List.of("p1", "p2"))
            physical.handleEvent(new PersonLeavesVehicleEvent(1000, Id.createPersonId(person), vehicle));
        var actual = rows(scheduled);
        assertEquals(rows(physical).stream().map(row -> String.join(";", row)).toList(),
                actual.stream().map(row -> String.join(";", row)).toList());
        assertEquals("00:00", actual.get(0)[0]); // Stop arrival, not boarding departure at 00:15.
        assertEquals("2", actual.get(0)[8]);
        assertEquals("00:15", actual.get(1)[0]);
        assertEquals("2", actual.get(1)[9]);
        assertEquals("line", actual.get(0)[3]); // Unnamed lines fall back to the ID.
    }

    @Test public void transfersAndSharedVehiclesCountEachRideAndResetBetweenIterations() throws Exception {
        var counter = handler(scenario(false));
        counter.handleEvent(ride("p", 1000, 910, "a", "c", "d0"));
        counter.handleEvent(ride("p", 1910, 1820, "a", "c", "d1"));
        var rows = rows(counter);
        assertEquals(4, rows.size());
        assertEquals(2, rows.stream().mapToInt(row -> Integer.parseInt(row[8])).sum());
        assertEquals(2, rows.stream().mapToInt(row -> Integer.parseInt(row[9])).sum());
        assertTrue(rows.stream().allMatch(row -> row[7].equals("shared")));
        counter.reset(2);
        assertTrue(rows(counter).isEmpty());
    }

    @Test public void serializedTimetableEventsAreAcceptedAndOtherGenericEventsIgnored() throws Exception {
        var counter = handler(scenario(false));
        var pt = ride("p", 1000, 910, "a", "c", "d0");
        var generic = new GenericEvent(PublicTransitEvent.TYPE, pt.getTime());
        generic.getAttributes().putAll(pt.getAttributes());
        counter.handleEvent(new GenericEvent("unrelated", 1));
        counter.handleEvent(generic);
        assertEquals(2, rows(counter).size());
    }

    @Test public void loopUsesTheActualBoardingVisitInsteadOfTheFirstStopOccurrence() throws Exception {
        var counter = handler(scenario(true));
        counter.handleEvent(ride("p", 1890, 1800, "a", "c", "d0"));
        var rows = rows(counter);
        assertEquals("00:15", rows.get(0)[0]); // Second arrival at a is 890 + 900.
        assertEquals("1", rows.get(0)[8]);
        assertEquals("00:30", rows.get(1)[0]);
    }

    private static PTPassengerCountingHandler handler(Scenario scenario) {
        return new PTPassengerCountsModule().providePTPassengerHandler(scenario);
    }

    private List<String[]> rows(PTPassengerCountingHandler handler) throws Exception {
        var output = temp.newFile("counts-" + System.nanoTime() + ".csv.gz").toPath();
        handler.writeCSV(output.toString());
        try (var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(output)),
                StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).map(line -> line.split(";", -1)).toList();
        }
    }

    private static PublicTransitEvent ride(String person, double arrival, double boarding, String from, String to,
            String departure) {
        return new PublicTransitEvent(arrival, Id.createPersonId(person), Id.create("line", TransitLine.class),
                Id.create("routeH", TransitRoute.class), stopId(from), stopId(to),
                Id.create(departure, Departure.class), boarding, 1000);
    }

    private static Id<TransitStopFacility> stopId(String id) {
        return Id.create(id, TransitStopFacility.class);
    }

    private static Scenario scenario(boolean loop) {
        var scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var schedule = scenario.getTransitSchedule();
        var factory = schedule.getFactory();
        var a = factory.createTransitStopFacility(stopId("a"), new Coord(0, 0), false);
        var c = factory.createTransitStopFacility(stopId("c"), new Coord(1000, 0), false);
        a.setName("A"); c.setName("C");
        schedule.addStopFacility(a); schedule.addStopFacility(c);
        var stops = loop ? List.of(factory.createTransitRouteStop(a, 0, 20),
                factory.createTransitRouteStop(a, 900, 910), factory.createTransitRouteStop(c, 1000, 1000))
                : List.of(factory.createTransitRouteStop(a, 0, 20), factory.createTransitRouteStop(c, 110, 110));
        var route = factory.createTransitRoute(Id.create("routeH", TransitRoute.class),
                RouteUtils.createLinkNetworkRouteImpl(Id.createLinkId("a"), Id.createLinkId("c")), stops, "bus");
        for (int i = 0; i < 2; i++) {
            var departure = factory.createDeparture(Id.create("d" + i, Departure.class), i == 0 ? 890 : 1800);
            departure.setVehicleId(Id.createVehicleId("shared"));
            route.addDeparture(departure);
        }
        var line = factory.createTransitLine(Id.create("line", TransitLine.class));
        line.addRoute(route); schedule.addTransitLine(line);
        return scenario;
    }
}
