package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.eqasim.core.components.flow.FlowBinManager;
import org.eqasim.core.components.flow.FlowConfigGroup;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.flow.FlowUtils;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.components.flow.VehiclePcuLookup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/** Detailed passages and daily flow agree only after matching event scope and bus weights. */
public class TestDetailedLinkFlowConsistency {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void linkEntriesMatchDailyCountsForRoadsWithAndWithoutSampleWeightedBuses() throws Exception {
        Scenario scenario = scenario();
        var detailed = new DetailedLinkStatisticsHandler(scenario);
        var flow = counter(scenario);
        var events = EventsUtils.createEventsManager(); events.addHandler(detailed); events.addHandler(flow); events.initProcessing();
        for (String vehicle : List.of("car", "truck", "bike", "bus")) {
            if (vehicle.equals("bus")) events.processEvent(new TransitDriverStartsEvent(0, Id.createPersonId("bus-driver"),
                    Id.createVehicleId(vehicle), Id.create("line", TransitLine.class), Id.create("bus-route", TransitRoute.class),
                    Id.create("departure", Departure.class)));
            String mode = vehicle.equals("bus") ? "pt" : vehicle;
            events.processEvent(new VehicleEntersTrafficEvent(10, Id.createPersonId(vehicle), Id.createLinkId("departure"),
                    Id.createVehicleId(vehicle), mode, 1));
            if (!vehicle.equals("bus")) events.processEvent(new LinkEnterEvent(100, Id.createVehicleId(vehicle), Id.createLinkId("no-bus")));
            events.processEvent(new LinkEnterEvent(200, Id.createVehicleId(vehicle), Id.createLinkId("with-bus")));
            events.processEvent(new VehicleLeavesTrafficEvent(300, Id.createPersonId(vehicle), Id.createLinkId("with-bus"),
                    Id.createVehicleId(vehicle), mode, 1));
        }
        events.finishProcessing();
        var rows = rows(detailed);
        var noBus = row(rows, "no-bus", 0); var withBus = row(rows, "with-bus", 0); var departure = row(rows, "departure", 0);
        assertEquals(1, value(noBus, "cars"), 0); assertEquals(1, value(noBus, "trucks"), 0);
        assertEquals(1, value(noBus, "bikes"), 0); assertEquals(0, value(noBus, "buses"), 0);
        assertEquals(value(noBus, "cars") + value(noBus, "trucks"), flow.getDailyCounts(Id.createLinkId("no-bus")), 0);
        assertEquals(1, value(withBus, "buses"), 0);
        assertEquals(value(withBus, "cars") + value(withBus, "trucks") + 0.1 * value(withBus, "buses"),
                flow.getDailyCounts(Id.createLinkId("with-bus")), 1e-6);
        // PCU flow is a different quantity: truck=3 and bus=0.3, whereas daily counts use 1 and 0.1.
        assertEquals(4.3, flow.getLinkCounts(Id.createLinkId("with-bus"), 200), 1e-6);
        assertEquals(1, value(departure, "cars"), 0); assertEquals(1, value(departure, "buses"), 0);
        assertEquals("Flow excludes VehicleEntersTrafficEvent; detailed passages include it", 0,
                flow.getDailyCounts(Id.createLinkId("departure")), 0);
        detailed.reset(1); flow.reset(1);
        assertTrue(rows(detailed).isEmpty()); assertEquals(0, flow.getDailyCounts(Id.createLinkId("with-bus")), 0);
    }

    @Test
    public void flowEndTimeIsInclusiveButDetailedStatisticsContinueToThirtyHours() throws Exception {
        Scenario scenario = scenario(); var detailed = new DetailedLinkStatisticsHandler(scenario); var flow = counter(scenario);
        var events = EventsUtils.createEventsManager(); events.addHandler(detailed); events.addHandler(flow); events.initProcessing();
        events.processEvent(new VehicleEntersTrafficEvent(0, Id.createPersonId("car"), Id.createLinkId("departure"), Id.createVehicleId("car"), "car", 1));
        for (double time : new double[]{86399, 86400, 86401, 107999, 108000})
            events.processEvent(new LinkEnterEvent(time, Id.createVehicleId("car"), Id.createLinkId("no-bus")));
        events.finishProcessing();
        var rows = rows(detailed);
        assertEquals(1, value(row(rows, "no-bus", 23), "cars"), 0);
        assertEquals(2, value(row(rows, "no-bus", 24), "cars"), 0);
        assertEquals(1, value(row(rows, "no-bus", 29), "cars"), 0);
        assertEquals("Flow includes the event at exactly 24h", 2, flow.getDailyCounts(Id.createLinkId("no-bus")), 0);
        assertTrue(rows.stream().noneMatch(row -> row.get("hour").equals("30")));
    }

    private Scenario scenario() {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        var network = scenario.getNetwork(); var factory = network.getFactory();
        var from = factory.createNode(Id.createNodeId("a"), new Coord(0, 0)); var to = factory.createNode(Id.createNodeId("b"), new Coord(100, 0));
        network.addNode(from); network.addNode(to);
        for (String name : List.of("departure", "no-bus", "with-bus")) network.addLink(factory.createLink(Id.createLinkId(name), from, to));
        for (String name : List.of("car", "truck", "bike", "bus")) {
            var type = VehicleUtils.createVehicleType(Id.create(name, VehicleType.class));
            type.setNetworkMode(name.equals("bus") ? "pt" : name); type.setPcuEquivalents(name.equals("bus") ? 0.3 : name.equals("truck") ? 3 : 1);
            var vehicles = name.equals("bus") ? scenario.getTransitVehicles() : scenario.getVehicles(); vehicles.addVehicleType(type);
            vehicles.addVehicle(VehicleUtils.createVehicle(Id.createVehicleId(name), type));
            if (!name.equals("bus")) scenario.getPopulation().addPerson(scenario.getPopulation().getFactory().createPerson(Id.createPersonId(name)));
        }
        var tf = scenario.getTransitSchedule().getFactory(); var line = tf.createTransitLine(Id.create("line", TransitLine.class));
        var route = tf.createTransitRoute(Id.create("bus-route", TransitRoute.class), null, List.of(), "bus");
        var departure = tf.createDeparture(Id.create("departure", Departure.class), 0); departure.setVehicleId(Id.createVehicleId("bus"));
        route.addDeparture(departure); line.addRoute(route); scenario.getTransitSchedule().addTransitLine(line);
        return scenario;
    }
    private LinkFlowCounter counter(Scenario scenario) {
        var config = new FlowConfigGroup(); var bins = new FlowBinManager(config); var utils = new FlowUtils(scenario);
        return new LinkFlowCounter(scenario.getNetwork(), new FlowDataSet(scenario.getNetwork(), bins, config.getBeta()), bins,
                mock(OutputDirectoryHierarchy.class), config, new VehiclePcuLookup(scenario, utils), 0.1, utils);
    }
    private List<Map<String, String>> rows(DetailedLinkStatisticsHandler handler) throws Exception {
        Path file = temporary.newFile().toPath(); handler.write(file.toString()); var csv = new CsvMapper();
        try (var input = new GZIPInputStream(Files.newInputStream(file)); MappingIterator<Map<String, String>> rows = csv.readerFor(
                csv.getTypeFactory().constructMapType(Map.class, String.class, String.class)).with(CsvSchema.emptySchema().withHeader()).readValues(input)) {
            return rows.readAll();
        }
    }
    private Map<String, String> row(List<Map<String, String>> rows, String link, int hour) {
        return rows.stream().filter(row -> row.get("link_id").equals(link) && Integer.parseInt(row.get("hour")) == hour).findFirst().orElseThrow();
    }
    private double value(Map<String, String> row, String name) { return Double.parseDouble(row.get(name)); }
}
