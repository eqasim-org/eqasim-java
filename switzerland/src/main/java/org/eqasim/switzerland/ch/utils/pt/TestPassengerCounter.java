package org.eqasim.switzerland.ch.utils.pt;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

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

import java.util.*;
import java.io.*;
import java.util.zip.GZIPInputStream;

@SuppressWarnings("unused")
public class TestPassengerCounter {

    public static void main(String[] args) throws Exception {
        // Path to your MATSim events file (compressed .xml.gz)
        String eventsFile   = "/home/asallard/Euler_scratch/eqasim/10pct/busSim_base/matsim.simulation.run__d00fb05e9ad96c568d20d46484e775e2.cache/simulation_output/ITERS/it.60/60.events.xml.gz";
        String outputCSV    = "/home/asallard/Euler_project/analysis/PT counts EventHandler/boarding_alighting_counts_10pct.csv.gz";
        String scheduleFile = "/home/asallard/Euler_scratch/eqasim/pt_simulated_expPlans/cache/matsim.simulation.run_from_inputs__30e7b11abb5ada20e82969241781f015.cache/directWalkFactor5/switzerland_transit_schedule.xml.gz";

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
                    if (vehicleId != null) {
                        vehicleToTripInfo.put(vehicleId, new TransitTripInfo(line.getId().toString(), route.getId().toString(), dep.getId().toString()));
                    }
                }
            }
        }

        // --- CREATE & REGISTER HANDLER ---
        EventsManager events = EventsUtils.createEventsManager();
        PTPassengerCountingHandler handler = new PTPassengerCountingHandler(vehicleToTripInfo);
        events.addHandler(handler);

        // --- READ EVENTS (GZ) ---
        new MatsimEventsReader(events).readFile(eventsFile);

        // --- WRITE RESULTS ---
        handler.writeCSV(outputCSV);
        System.out.println("Done! CSV written to: " + outputCSV);
    }
}