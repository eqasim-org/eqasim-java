package org.eqasim.switzerland.ch_cmdp.utils.link_stats;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.vehicles.Vehicle;

/**
 * Counts vehicle passages, not occupancy or unique people. Each car passage is
 * assigned to its driver, with precedence freight > cross-border > French > Swiss.
 * Unknown drivers remain explicit rather than being labelled Swiss. Only network
 * modes with link events can be counted (teleported bikes cannot).
 */
final class DetailedLinkStatisticsHandler implements LinkEnterEventHandler,
        VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, TransitDriverStartsEventHandler {
    private enum Category { SWISS, FRENCH, CROSS_BORDER, UNKNOWN_CAR, TRUCK, BIKE, BUS, IGNORE }
    private static final int BIN_SIZE_SECONDS = 3600;
    private static final int END_TIME_SECONDS = 30 * BIN_SIZE_SECONDS;
    private static final int COUNTED_CATEGORIES = Category.IGNORE.ordinal();

    private final Scenario scenario;
    private final Map<Id<Vehicle>, Category> activeVehicles = new HashMap<>();
    // Route-specific classification handles vehicles reused for different transit modes.
    private final Map<Id<Vehicle>, Category> transitVehicles = new HashMap<>();
    // One counter per category (excluding IGNORE), for each observed link/hour.
    private final Map<Integer, Map<Id<Link>, int[]>> counts = new TreeMap<>();

    DetailedLinkStatisticsHandler(Scenario scenario) {
        this.scenario = scenario;
    }

    @Override
    public void reset(int iteration) {
        activeVehicles.clear();
        transitVehicles.clear();
        counts.clear();
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        var line = scenario.getTransitSchedule().getTransitLines().get(event.getTransitLineId());
        var route = line == null ? null : line.getRoutes().get(event.getTransitRouteId());
        transitVehicles.put(event.getVehicleId(), route != null && "bus".equals(route.getTransportMode())
                ? Category.BUS : Category.IGNORE);
    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        Category category = transitVehicles.get(event.getVehicleId());
        if (category == null) {
            category = classify(event);
        }
        activeVehicles.remove(event.getVehicleId());
        if (category != Category.IGNORE) {
            activeVehicles.put(event.getVehicleId(), category);
            // MATSim emits no LinkEnterEvent for the departure link.
            count(event.getTime(), event.getLinkId(), category);
        }
    }

    private Category classify(VehicleEntersTrafficEvent event) {
        String mode = event.getNetworkMode();
        if (TransportMode.truck.equals(mode)) return Category.TRUCK;
        if (TransportMode.bike.equals(mode)) return Category.BIKE;
        if ("bus".equals(mode)) return Category.BUS;
        if (!TransportMode.car.equals(mode)) return Category.IGNORE;

        Person person = scenario.getPopulation().getPersons().get(event.getPersonId());
        if (person == null) return Category.UNKNOWN_CAR;
        if (flag(person, "isFreight")) return Category.TRUCK;
        if (flag(person, "isCrossBorder")) return Category.CROSS_BORDER;
        if (flag(person, "isExternalFR")) return Category.FRENCH;
        return Category.SWISS;
    }

    private static boolean flag(Person person, String name) {
        Object value = person.getAttributes().getAttribute(name);
        return Boolean.TRUE.equals(value) || (value instanceof String text && Boolean.parseBoolean(text));
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        Category category = activeVehicles.get(event.getVehicleId());
        if (category != null) {
            count(event.getTime(), event.getLinkId(), category);
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        activeVehicles.remove(event.getVehicleId());
        transitVehicles.remove(event.getVehicleId());
    }

    private void count(double time, Id<Link> link, Category category) {
        if (!Double.isFinite(time) || time < 0 || time >= END_TIME_SECONDS) return;
        int hour = (int) (time / BIN_SIZE_SECONDS);
        counts.computeIfAbsent(hour, ignored -> new HashMap<>())
                .computeIfAbsent(link, ignored -> new int[COUNTED_CATEGORIES])[category.ordinal()]++;
    }

    void write(String filename) throws IOException {
        try (var gzip = new GZIPOutputStream(Files.newOutputStream(Path.of(filename)), 64 * 1024);
                var writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8), 64 * 1024)) {
            writer.write("link_id,hour,start_time_s,end_time_s,cars,trucks,bikes,buses,"
                    + "cars_french,cars_crossborder,cars_swiss,cars_unknown\n");
            for (var hourEntry : counts.entrySet()) {
                int hour = hourEntry.getKey();
                for (var linkEntry : hourEntry.getValue().entrySet()) {
                    int[] c = linkEntry.getValue();
                    int cars = c[0] + c[1] + c[2] + c[3];
                    writer.write(csv(linkEntry.getKey().toString()) + "," + hour + "," + hour * BIN_SIZE_SECONDS
                            + "," + (hour + 1) * BIN_SIZE_SECONDS + "," + cars + "," + c[4] + "," + c[5]
                            + "," + c[6] + "," + c[1] + "," + c[2] + "," + c[0] + "," + c[3] + "\n");
                }
            }
        }
    }

    private static String csv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
