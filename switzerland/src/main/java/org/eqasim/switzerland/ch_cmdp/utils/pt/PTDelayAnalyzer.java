package org.eqasim.switzerland.ch_cmdp.utils.pt;
import org.matsim.api.core.v01.events.Event;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.events.handler.BasicEventHandler;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PTDelayAnalyzer implements BasicEventHandler {
    
    private final List<PtDelayEvent> delayEvents = new ArrayList<>();

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: PtDelayAnalyzer <input_events.xml.gz> <output_delays.csv>");
            System.exit(1);
        }

        // Create event manager and handler
        PTDelayAnalyzer analyzer = new PTDelayAnalyzer();
        var eventsManager = EventsUtils.createEventsManager();
        eventsManager.addHandler(analyzer);

        // Read events
        new MatsimEventsReader(eventsManager).readFile(args[0]);
        
        // Write results
        analyzer.writeCsv(args[1]);
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getEventType().equals("VehicleArrivesAtFacility") || 
            event.getEventType().equals("VehicleDepartsAtFacility")) {

                String vehicleId = event.getAttributes().get("vehicle");

                if ("veh_22696_rail".equals(vehicleId)) {
                    System.out.println("Found target vehicle event at time: " + event.getTime());
                }
            
            PtDelayEvent delayEvent = new PtDelayEvent(
                (int) event.getTime(),
                event.getEventType(),
                event.getAttributes().get("vehicle"),
                event.getAttributes().get("facility"),
                (int) Double.parseDouble(event.getAttributes().getOrDefault("delay", "0"))
            );
            delayEvents.add(delayEvent);
        }
    }

    private void writeCsv(String filename) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write("eventTime,eventType,vehicleId,linkId,delay\n");
            for (PtDelayEvent event : delayEvents) {
                writer.write(String.format("%d,%s,%s,%s,%d\n",
                    event.time,
                    event.eventType,
                    event.vehicleId,
                    event.linkId,
                    event.delay));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class PtDelayEvent {
        final int time;
        final String eventType;
        final String vehicleId;
        final String linkId;
        final int delay;

        private static String extractLinkId(String facilityId) {
            if (facilityId == null) return "";
            int idx = facilityId.indexOf("link:");
            if (idx >= 0) {
                // Get the substring after "link:"
                String afterLink = facilityId.substring(idx + 5);
                // If there are more delimiters, take only the integer part
                int nextColon = afterLink.indexOf(':');
                if (nextColon >= 0) {
                    return afterLink.substring(0, nextColon);
                } else {
                    return afterLink;
                }
            }
            return "";
        }

        PtDelayEvent(int time, String eventType, String vehicleId, String facilityId, int delay) {
            this.time = time;
            this.eventType = eventType;
            this.vehicleId = vehicleId;
            this.linkId = extractLinkId(facilityId);
            this.delay = delay;
        }
    }

}

