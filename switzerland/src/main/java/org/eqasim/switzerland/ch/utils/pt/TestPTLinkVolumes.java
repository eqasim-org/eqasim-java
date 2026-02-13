package org.eqasim.switzerland.ch.utils.pt;

import java.util.*;
import java.util.zip.GZIPInputStream;
import java.io.*;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.vehicles.Vehicle;

@SuppressWarnings("unused")
public class TestPTLinkVolumes {

    public static void main(String[] args) throws Exception {
        Map<String, String> argMap = parseArguments(args);

        String eventsFile    = requireArg(argMap, "--events-path");
        String outputCSV     = requireArg(argMap, "--output-path");
        String scheduleFile  = requireArg(argMap, "--transit-schedule-path");

        // --- LOAD TRANSIT SCHEDULE (GZ) ---
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitScheduleReader scheduleReader = new TransitScheduleReader(scenario);

        try (InputStream in = new GZIPInputStream(new FileInputStream(scheduleFile))) {
            scheduleReader.readStream(in);
        }

        TransitSchedule schedule = scenario.getTransitSchedule();

        // --- BUILD vehicleId → (lineId, routeId, departureId) MAP ---
        Map<Id<Vehicle>, TransitTripInfo> vehicleToTripInfo = new HashMap<>();

        for (TransitLine line : schedule.getTransitLines().values()) {
            for (TransitRoute route : line.getRoutes().values()) {
                for (Departure dep : route.getDepartures().values()) {
                    Id<Vehicle> vehicleId = dep.getVehicleId();
                    List<String> routeLineInfo = TransitTripInfo.findLineRouteInfo(line, route);
                    if (vehicleId != null) {
                        vehicleToTripInfo.put(vehicleId, new TransitTripInfo(line.getId().toString(), line.getName().toString() , route.getId().toString(), dep.getId().toString(), routeLineInfo.get(0), routeLineInfo.get(1)));
                    }
                }
            }
        }

        // --- CREATE & REGISTER HANDLER ---
        EventsManager events = EventsUtils.createEventsManager();
        PTLinkVolumesHandler handler = new PTLinkVolumesHandler(vehicleToTripInfo);
        events.addHandler(handler);

        // --- READ EVENTS (GZ) ---
        new MatsimEventsReader(events).readFile(eventsFile);

        // --- WRITE RESULTS ---
        handler.writeCSV(outputCSV);
        System.out.println("Done! CSV written to: " + outputCSV);
    }

    private static Map<String, String> parseArguments(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < args.length - 1; i += 2) {
            map.put(args[i], args[i + 1]);
        }
        return map;
    }

    private static String requireArg(Map<String, String> args, String key) {
        if (!args.containsKey(key)) {
            System.err.println("Missing required argument: " + key);
            System.exit(1);
        }
        return args.get(key);
    }
    
}
