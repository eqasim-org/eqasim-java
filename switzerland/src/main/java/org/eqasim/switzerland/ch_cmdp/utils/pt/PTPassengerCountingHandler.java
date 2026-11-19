package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;


public class PTPassengerCountingHandler implements VehicleArrivesAtFacilityEventHandler, VehicleDepartsAtFacilityEventHandler, 
        PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, GenericEventHandler {

    private static final int BIN_SIZE = 900; // 15 minutes in seconds
    private final Map<Id<Vehicle>, ArrivalInfo> vehicleArrivalMap = new HashMap<>();
    private final Map<BinKey, Counter> binCounts = new HashMap<>();
    private final Map<Id<Vehicle>, TransitTripInfo> vehicleToLineRoute;
    private final TransitSchedule schedule;
    private final Map<TransitRoute, TransitTripInfo> scheduledRouteInfo = new HashMap<>();

    public PTPassengerCountingHandler(Map<Id<Vehicle>, TransitTripInfo> vehicleToLineRoute) {
        this(vehicleToLineRoute, null);
    }

    /** The schedule resolves timetable ride events, which do not have physical vehicle events. */
    public PTPassengerCountingHandler(Map<Id<Vehicle>, TransitTripInfo> vehicleToLineRoute, TransitSchedule schedule) {
        this.vehicleToLineRoute = vehicleToLineRoute;
        this.schedule = schedule;
    }

    static class ArrivalInfo {
        int timeBin;
        double time;
        Id<TransitStopFacility> stopId;
        String lineId;  
        String lineName; 
        String routeId;  
        String departureId; 
        Id<Vehicle> vehicleId;
        String routeDirection;
        String lineMainDirection;
    }

    static class BinKey {
        int timeBin;
        int time;
        Id<TransitStopFacility> stopId;
        String lineId;
        String lineName;
        String routeId;
        String departureId;
        Id<Vehicle> vehicleId; 
        String routeDirection;
        String lineMainDirection;

        BinKey(int bin, double time, Id<TransitStopFacility> stopId, String lineId, String lineName, String routeId, String departureId, Id<Vehicle> vehicleId, String routeDirection, String lineMainDirection) {
            this.timeBin = bin;
            this.time = (int) time;
            this.stopId = stopId;
            this.lineId = lineId;
            this.lineName = lineName;
            this.routeId = routeId;
            this.departureId = departureId;
            this.vehicleId = vehicleId;
            this.routeDirection = routeDirection;
            this.lineMainDirection = lineMainDirection;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof BinKey)) return false;
            BinKey b = (BinKey) o;
            return time == b.time &&
                   Objects.equals(stopId, b.stopId) &&
                   Objects.equals(lineId, b.lineId) &&
                   Objects.equals(routeId, b.routeId)&&
                   Objects.equals(departureId, b.departureId) &&
                   Objects.equals(vehicleId, b.vehicleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(time, stopId, lineId, routeId, departureId, vehicleId);
        }
    }

    static class Counter {
        int boardings = 0;
        int alightings = 0;
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        int timeBin = (int) event.getTime() / BIN_SIZE;

        ArrivalInfo info = new ArrivalInfo();
        info.timeBin = timeBin;
        info.time = event.getTime();
        info.stopId = event.getFacilityId();
        info.vehicleId = event.getVehicleId();
        TransitTripInfo lineRoute = vehicleToLineRoute.get(info.vehicleId);
        info.lineId = (lineRoute != null) ? lineRoute.lineId : "unknown";
        info.lineName = (lineRoute != null) ? lineRoute.lineName : "unknown";
        info.routeId = (lineRoute != null) ? lineRoute.routeId : "unknown";
        info.departureId = (lineRoute != null) ? lineRoute.departureId : "unknown";
        info.routeDirection = (lineRoute != null) ? lineRoute.routeDirection : "unknown";
        info.lineMainDirection = (lineRoute != null) ? lineRoute.routeMainDirection : "unknown";

        vehicleArrivalMap.put(event.getVehicleId(), info);

        BinKey key = new BinKey(timeBin, info.time, info.stopId, info.lineId, info.lineName, info.routeId, info.departureId, info.vehicleId, info.routeDirection, info.lineMainDirection);
        binCounts.putIfAbsent(key, new Counter()); 
    }

    @Override
    public void handleEvent(VehicleDepartsAtFacilityEvent event) {
        // Remove the vehicle from tracking: we no longer count boardings/alightings
        vehicleArrivalMap.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        ArrivalInfo info = vehicleArrivalMap.get(event.getVehicleId());
        if (info == null) return; // not a PT vehicle

        BinKey key = new BinKey(info.timeBin, info.time, info.stopId, info.lineId, info.lineName, info.routeId, info.departureId, info.vehicleId, info.routeDirection, info.lineMainDirection);
        binCounts.computeIfAbsent(key, k -> new Counter()).boardings++;
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        ArrivalInfo info = vehicleArrivalMap.get(event.getVehicleId());
        if (info == null) return;

        BinKey key = new BinKey(info.timeBin, info.time, info.stopId, info.lineId, info.lineName, info.routeId, info.departureId, info.vehicleId, info.routeDirection, info.lineMainDirection);
        binCounts.computeIfAbsent(key, k -> new Counter()).alightings++;
    }

    /** One completed timetable ride contributes one boarding and one alighting, also for transfers. */
    @Override
    public void handleEvent(GenericEvent event) {
        if (!PublicTransitEvent.TYPE.equals(event.getEventType())) return;
        if (schedule == null) {
            throw new IllegalStateException("A transit schedule is required to count pt_transit events");
        }
        PublicTransitEvent pt = event instanceof PublicTransitEvent transit ? transit : PublicTransitEvent.convert(event);
        var line = schedule.getTransitLines().get(pt.getTransitLineId());
        var route = line.getRoutes().get(pt.getTransitRouteId());
        var departure = route.getDepartures().get(pt.getDepartureId());
        // Cache direction labels per route, not per passenger or departure.
        var metadata = scheduledRouteInfo.computeIfAbsent(route, key -> {
            var directions = TransitTripInfo.findLineRouteInfo(line, route);
            return new TransitTripInfo(line.getId().toString(),
                    line.getName() == null ? line.getId().toString() : line.getName(), route.getId().toString(),
                    "", directions.get(0), directions.get(1));
        });
        // vehicleDepartureTime is the departure from the boarding stop, not from the route origin.
        // Match its offset so repeated visits to the same stop on a loop are distinguished.
        var boardingStop = route.getStops().stream()
                .filter(stop -> stop.getStopFacility().getId().equals(pt.getAccessStopId()))
                .filter(stop -> Math.abs(departure.getDepartureTime()
                        + stop.getDepartureOffset().orElse(Double.NaN) - pt.getVehicleDepartureTime()) < 1e-6)
                .findFirst().orElseThrow(() -> new IllegalArgumentException(
                        "Cannot resolve boarding stop for pt_transit on " + route.getId() + "/" + departure.getId()));
        // Physical vehicle events group boardings by vehicle arrival at the stop. Use the same convention.
        double boardingArrival = departure.getDepartureTime()
                + boardingStop.getArrivalOffset().orElse(boardingStop.getDepartureOffset().seconds());
        timetableCounter(boardingArrival, pt.getAccessStopId(), metadata, departure.getId().toString(),
                departure.getVehicleId()).boardings++;
        timetableCounter(pt.getTime(), pt.getEgressStopId(), metadata, departure.getId().toString(),
                departure.getVehicleId()).alightings++;
    }

    private Counter timetableCounter(double time, Id<TransitStopFacility> stop, TransitTripInfo info,
            String departure, Id<Vehicle> vehicle) {
        var key = new BinKey((int) time / BIN_SIZE, time, stop, info.lineId, info.lineName, info.routeId,
                departure, vehicle, info.routeDirection, info.routeMainDirection);
        return binCounts.computeIfAbsent(key, ignored -> new Counter());
    }

    @Override
    public void reset(int iteration) {
        vehicleArrivalMap.clear();
        binCounts.clear();
        scheduledRouteInfo.clear();
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }
    
    public void writeCSV(String path) throws IOException {
        boolean isGzipped = path.endsWith(".gz");

        OutputStream outputStream = isGzipped
            ? new GZIPOutputStream(new FileOutputStream(path))
            : new FileOutputStream(path);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            // Header
            writer.write("time_bin;stop_id;line_id;line_name;route_id;route_direction;line_main_direction;vehicle_id;boardings;alightings\n");

            // Sort entries by time
            List<Map.Entry<BinKey, Counter>> sortedEntries = new ArrayList<>(binCounts.entrySet());
            sortedEntries.sort(Comparator.comparingInt(e -> e.getKey().time));

            for (Map.Entry<BinKey, Counter> entry : sortedEntries) {
                BinKey k = entry.getKey();
                Counter c = entry.getValue();
                String timeStr = formatTime(k.timeBin * BIN_SIZE);
                if (c.boardings > 0 || c.alightings > 0){
                    writer.write(String.format("%s;%s;%s;%s;%s;%s;%s;%s;%d;%d\n",
                        timeStr, k.stopId, k.lineId, k.lineName, k.routeId, k.routeDirection, k.lineMainDirection, k.vehicleId,
                        c.boardings, c.alightings));
                }
            }
            writer.flush();
            writer.close();
        }
        
    }
    
}


