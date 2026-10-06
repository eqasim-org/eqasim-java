package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.GZIPInputStream;

import ch.sbb.matsim.config.SBBTransitConfigGroup;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestDetailedLinkStatistics {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Scenario scenario() {
        return ScenarioUtils.createScenario(ConfigUtils.createConfig());
    }

    private Person person(Scenario scenario, String id, String attribute, Object value) {
        Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
        if (attribute != null) person.getAttributes().putAttribute(attribute, value);
        scenario.getPopulation().addPerson(person);
        return person;
    }

    private void enter(DetailedLinkStatisticsHandler handler, double time, String driver, String vehicle, String mode) {
        handler.handleEvent(new VehicleEntersTrafficEvent(time, Id.createPersonId(driver),
                Id.createLinkId("link"), Id.createVehicleId(vehicle), mode, 1));
    }

    private List<String> rows(DetailedLinkStatisticsHandler handler) throws Exception {
        Path file = temporary.newFile().toPath();
        handler.write(file.toString());
        return read(file);
    }

    private List<String> read(Path file) throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(
                new GZIPInputStream(Files.newInputStream(file)), StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        }
    }

    @Test
    public void classifiesModesAndDriverFlagsWithoutCountingPassengers() throws Exception {
        Scenario scenario = scenario();
        person(scenario, "swiss", "isExternalFR", false);
        person(scenario, "french", "isExternalFR", "true");
        Person cross = person(scenario, "cross", "isCrossBorder", true);
        cross.getAttributes().putAttribute("isExternalFR", true);
        Person freight = person(scenario, "freight", "isFreight", true);
        freight.getAttributes().putAttribute("isCrossBorder", true);
        var handler = new DetailedLinkStatisticsHandler(scenario);
        enter(handler, 0, "swiss", "a", "car");
        enter(handler, 0, "french", "b", "car");
        enter(handler, 0, "cross", "c", "car");
        enter(handler, 0, "missing", "d", "car");
        enter(handler, 0, "freight", "e", "car");
        enter(handler, 0, "missing", "f", "truck");
        enter(handler, 0, "swiss", "g", "bike");
        enter(handler, 0, "missing", "h", "bus");
        enter(handler, 0, "missing", "i", "rail");
        assertEquals(List.of("\"link\",0,0,3600,4,2,1,1,1,1,1,1"), rows(handler).subList(1, 2));
    }

    @Test
    public void countsStartLinksHourBoundariesRepeatedPassagesAndVehicleReuse() throws Exception {
        Scenario scenario = scenario();
        person(scenario, "swiss", null, null);
        person(scenario, "french", "isExternalFR", true);
        var handler = new DetailedLinkStatisticsHandler(scenario);
        enter(handler, 3599, "swiss", "v", "car");
        handler.handleEvent(new LinkEnterEvent(3600, Id.createVehicleId("v"), Id.createLinkId("link")));
        handler.handleEvent(new LinkEnterEvent(3601, Id.createVehicleId("v"), Id.createLinkId("link")));
        handler.handleEvent(new VehicleLeavesTrafficEvent(3602, Id.createPersonId("swiss"),
                Id.createLinkId("link"), Id.createVehicleId("v"), "car", 1));
        handler.handleEvent(new LinkEnterEvent(3603, Id.createVehicleId("v"), Id.createLinkId("ignored")));
        enter(handler, 90000, "french", "v", "car");
        var rows = rows(handler);
        assertEquals(4, rows.size());
        assertEquals("\"link\",0,0,3600,1,0,0,0,0,0,1,0", rows.get(1));
        assertEquals("\"link\",1,3600,7200,2,0,0,0,0,0,2,0", rows.get(2));
        assertEquals("\"link\",25,90000,93600,1,0,0,0,1,0,0,0", rows.get(3));
        handler.reset(1);
        handler.handleEvent(new LinkEnterEvent(1, Id.createVehicleId("v"), Id.createLinkId("link")));
        assertEquals(1, rows(handler).size());
    }

    @Test
    public void ignoresEntriesAtOrAfterThirtyHours() throws Exception {
        Scenario scenario = scenario();
        person(scenario, "swiss", null, null);
        var handler = new DetailedLinkStatisticsHandler(scenario);
        enter(handler, 29 * 3600, "swiss", "v", "car");
        handler.handleEvent(new LinkEnterEvent(108000 - 0.1, Id.createVehicleId("v"), Id.createLinkId("link")));
        handler.handleEvent(new LinkEnterEvent(108000, Id.createVehicleId("v"), Id.createLinkId("link")));
        handler.handleEvent(new LinkEnterEvent(111600, Id.createVehicleId("v"), Id.createLinkId("link")));
        enter(handler, 108000, "swiss", "late", "car");
        enter(handler, 111600, "swiss", "later", "car");
        var rows = rows(handler);
        assertEquals(2, rows.size());
        assertEquals("\"link\",29,104400,108000,2,0,0,0,0,0,2,0", rows.get(1));
    }

    @Test
    public void identifiesBusesFromRoutesEvenWhenNetworkModeIsPt() throws Exception {
        Scenario scenario = scenario();
        var factory = scenario.getTransitSchedule().getFactory();
        var line = factory.createTransitLine(Id.create("line", TransitLine.class));
        line.addRoute(factory.createTransitRoute(Id.create("bus", TransitRoute.class), null, List.of(), "bus"));
        line.addRoute(factory.createTransitRoute(Id.create("train", TransitRoute.class), null, List.of(), "rail"));
        scenario.getTransitSchedule().addTransitLine(line);
        var handler = new DetailedLinkStatisticsHandler(scenario);
        for (String route : List.of("bus", "train")) {
            handler.handleEvent(new TransitDriverStartsEvent(0, Id.createPersonId("driver"),
                    Id.createVehicleId("v"), line.getId(), Id.create(route, TransitRoute.class),
                    Id.create("departure", Departure.class)));
            enter(handler, 0, "driver", "v", "pt");
            handler.handleEvent(new LinkEnterEvent(1, Id.createVehicleId("v"), Id.createLinkId("link")));
            handler.handleEvent(new VehicleLeavesTrafficEvent(2, Id.createPersonId("driver"),
                    Id.createLinkId("link"), Id.createVehicleId("v"), "pt", 1));
        }
        assertEquals("\"link\",0,0,3600,0,0,0,2,0,0,0,0", rows(handler).get(1));
    }

    @Test
    public void registersOnlyInFinalIterationAndRestoresSbbSettings() throws Exception {
        Scenario scenario = scenario();
        var sbb = ConfigUtils.addOrGetModule(scenario.getConfig(), SBBTransitConfigGroup.class);
        sbb.setCreateLinkEventsInterval(0);
        var services = mock(MatsimServices.class);
        var events = mock(EventsManager.class);
        var output = mock(OutputDirectoryHierarchy.class);
        Path file = temporary.getRoot().toPath().resolve("output_detailed_link_statistics.csv.gz");
        when(services.getEvents()).thenReturn(events);
        when(output.getOutputFilename("output_detailed_link_statistics.csv.gz")).thenReturn(file.toString());
        var listener = new DetailedLinkStatisticsModule.Listener(scenario, output);
        listener.notifyIterationStarts(new IterationStartsEvent(services, 0, false));
        listener.notifyIterationEnds(new IterationEndsEvent(services, 0, false));
        verifyNoInteractions(events);
        assertFalse(Files.exists(file));
        assertEquals(0, sbb.getCreateLinkEventsInterval());
        listener.notifyIterationStarts(new IterationStartsEvent(services, 1, true));
        verify(events).addHandler(any(DetailedLinkStatisticsHandler.class));
        assertEquals(1, sbb.getCreateLinkEventsInterval());
        listener.notifyIterationEnds(new IterationEndsEvent(services, 1, true));
        verify(events).removeHandler(any(DetailedLinkStatisticsHandler.class));
        assertEquals(0, sbb.getCreateLinkEventsInterval());
        assertEquals(1, read(file).size());
    }
}
