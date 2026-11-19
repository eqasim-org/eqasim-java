package org.eqasim.switzerland.ch_cmdp.utils.pt;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.events.EventsUtils;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class TestPTPassengerAnalyser {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test public void oddLastIterationWritesMidpointAndFinalWithoutAccumulatingOtherIterations() throws Exception {
        run(0, 5);
    }

    @Test public void evenLastIterationWritesMidpointAndFinal() throws Exception {
        run(0, 4);
    }

    @Test public void singleIterationWritesFinalOnlyOnce() throws Exception {
        run(0, 0);
    }

    @Test public void restartAfterMidpointWritesOnlyFinal() throws Exception {
        run(4, 5);
    }

    private void run(int first, int last) throws Exception {
        var config = ConfigUtils.createConfig();
        config.controller().setFirstIteration(first);
        config.controller().setLastIteration(last);
        var services = mock(MatsimServices.class);
        var output = mock(OutputDirectoryHierarchy.class);
        var events = EventsUtils.createEventsManager();
        var midpoint = temporary.getRoot().toPath().resolve("ITERS/it." + last / 2)
                .resolve(last / 2 + ".pt_passenger_counts.csv.gz");
        var finalFile = temporary.getRoot().toPath().resolve("pt_passenger_counts.csv.gz");
        when(services.getConfig()).thenReturn(config);
        when(services.getEvents()).thenReturn(events);
        when(services.getControlerIO()).thenReturn(output);
        when(output.getOutputFilename("pt_passenger_counts.csv.gz")).thenReturn(finalFile.toString());
        when(output.getIterationFilename(last / 2, "pt_passenger_counts.csv.gz")).thenReturn(midpoint.toString());
        var vehicle = Id.createVehicleId("bus");
        var counter = new PTPassengerCountingHandler(Map.of(vehicle,
                new TransitTripInfo("line", "Line", "route", "departure", "A->B", "A->B")));
        var listener = new PTPassengerAnalyser(counter);
        for (int iteration = first; iteration <= last; iteration++) {
            listener.notifyIterationStarts(new IterationStartsEvent(services, iteration, iteration == last));
            events.initProcessing();
            events.processEvent(new VehicleArrivesAtFacilityEvent(100, vehicle,
                    Id.create("stop", TransitStopFacility.class), 0));
            // Different counts expose carry-over and unwanted registration in other iterations.
            for (int i = 0; i <= iteration; i++) {
                events.processEvent(new PersonEntersVehicleEvent(101, Id.createPersonId("p" + i), vehicle));
            }
            events.finishProcessing();
            listener.notifyIterationEnds(new IterationEndsEvent(services, iteration, iteration == last));
            assertEquals(iteration == last, Files.exists(finalFile));
        }
        assertEquals(last + 1, boardings(finalFile));
        if (last / 2 >= first && last / 2 != last) {
            assertEquals(last / 2 + 1, boardings(midpoint));
            verify(output).getIterationFilename(last / 2, "pt_passenger_counts.csv.gz");
        } else {
            assertFalse(Files.exists(midpoint));
            verify(output, never()).getIterationFilename(anyInt(), anyString());
        }
        verify(output).getOutputFilename("pt_passenger_counts.csv.gz");
        var empty = temporary.getRoot().toPath().resolve("cleared.csv.gz");
        counter.writeCSV(empty.toString());
        assertEquals(0, boardings(empty));
    }

    private static int boardings(Path file) throws Exception {
        try (var reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(file)),
                StandardCharsets.UTF_8))) {
            return reader.lines().skip(1).mapToInt(row -> Integer.parseInt(row.split(";", -1)[8])).sum();
        }
    }
}
