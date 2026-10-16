package org.eqasim.core.components.travel_time;

import java.io.*;
import java.util.Map;
import java.util.concurrent.Executors;
import org.eqasim.core.scenario.cutter.network.RoadNetwork;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import static org.junit.Assert.*;

public class TestRecordedFloatTravelTime {
    @Test public void legacyDoubleFilesReadIntoFloatsAndWriteCompatibleDoubleFiles() throws Exception {
        var bytes = new ByteArrayOutputStream();
        var writer = new DataOutputStream(bytes);
        writer.writeDouble(0); writer.writeDouble(1800); writer.writeDouble(900);
        writer.writeInt(2); writer.writeInt(1); writer.writeUTF("ab");
        writer.writeDouble(12.3456789); writer.writeDouble(23.456789);
        var times = RecordedTravelTime.readBinary(new ByteArrayInputStream(bytes.toByteArray()));
        var link = TestFloatTravelTimeCalculator.network().getLinks().get(Id.createLinkId("ab"));
        assertEquals((double) (float) 12.3456789, times.getLinkTravelTime(link, -1, null, null), 0);
        assertEquals((double) (float) 23.456789, times.getLinkTravelTime(link, 100000, null, null), 0);
        assertEquals((double) (float) 12.3456789, times.getMinimumLinkTravelTime(link), 0);
        var field = RecordedTravelTime.class.getDeclaredField("data"); field.setAccessible(true);
        assertTrue(((Map<?, ?>) field.get(times)).get(link.getId()) instanceof float[]);
        bytes.reset(); RecordedTravelTime.writeBinary(bytes, times);
        var reader = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(0, reader.readDouble(), 0); assertEquals(1800, reader.readDouble(), 0);
        assertEquals(900, reader.readDouble(), 0); assertEquals(2, reader.readInt());
        assertEquals(1, reader.readInt()); assertEquals("ab", reader.readUTF());
        assertEquals((double) (float) 12.3456789, reader.readDouble(), 0);
        assertEquals((double) (float) 23.456789, reader.readDouble(), 0);
        assertEquals(-1, reader.read());
    }

    @Test public void recorderSnapshotsAreIndependentAndSharedReadsDoNotMutateThem() throws Exception {
        var network = new RoadNetwork(TestFloatTravelTimeCalculator.network());
        var recorder = new TravelTimeRecorder(network, 0, 1900, 900);
        var link = network.getLinks().get(Id.createLinkId("ab"));
        var vehicle = Id.createVehicleId("test");
        recorder.handleEvent(new LinkEnterEvent(100, vehicle, link.getId()));
        recorder.handleEvent(new LinkLeaveEvent(112.5, vehicle, link.getId()));
        var snapshot = recorder.getTravelTime();
        recorder.handleEvent(new LinkEnterEvent(100, vehicle, link.getId()));
        recorder.handleEvent(new LinkLeaveEvent(137.5, vehicle, link.getId()));
        assertEquals(25, recorder.getTravelTime().getLinkTravelTime(link, 100, null, null), 0);
        try (var pool = Executors.newFixedThreadPool(4)) {
            var tasks = new java.util.ArrayList<java.util.concurrent.Callable<Double>>();
            for (int i = 0; i < 8; i++) tasks.add(() -> {
                double total = 0;
                for (int j = 0; j < 10000; j++) total += snapshot.getLinkTravelTime(link, 100, null, null);
                return total;
            });
            for (var result : pool.invokeAll(tasks)) assertEquals(125000, result.get(), 0);
        }
        assertEquals(10, snapshot.getLinkTravelTime(link, 1850, null, null), 0);
    }
}
