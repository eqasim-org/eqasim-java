package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.vehicles.Vehicle;


public class PTLinkVolumesHandler implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler {

    private static final int BIN_SIZE = 900; // 15 minutes in seconds
    private final Map<Id<Vehicle>, TransitTripInfo> vehicleToLineRoute;
    private final Map<Id<Vehicle>, Integer> vehicleNbPassengers = new HashMap<>();
    private final Map<LinkBinKey, Integer> linkNbPassengers     = new HashMap<>();

    public PTLinkVolumesHandler(Map<Id<Vehicle>, TransitTripInfo> vehicleToLineRoute) {
        this.vehicleToLineRoute = vehicleToLineRoute;
    }

    static class LinkBinKey {
        int timeBin;
        Id<Link> linkId;
        String lineId;
        String lineName;
        String routeId;
        String routeDirection;
        String lineMainDirection;

        LinkBinKey(int timeBin, Id<Link> linkId, String lineId, String lineName, String routeId, String routeDirection, String lineMainDirection){
            this.timeBin = timeBin;
            this.linkId = linkId;
            this.lineId = lineId;
            this.routeId = routeId;
            this.lineName = lineName;
            this.routeDirection = routeDirection;
            this.lineMainDirection = lineMainDirection;
        }

        @Override
        public boolean equals(Object o){
            if (!(o instanceof LinkBinKey)) return false;
            LinkBinKey l = (LinkBinKey) o;
            return timeBin == l.timeBin && 
                    Objects.equals(linkId, l.linkId) &&
                    Objects.equals(lineId, l.lineId) &&
                    Objects.equals(lineName, l.lineName) &&
                    Objects.equals(routeId, l.routeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeBin, linkId, lineId, lineName, routeId);
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        int timeBin = (int) event.getTime() / BIN_SIZE;
        Id<Vehicle> vehicleId = event.getVehicleId();

        int currentNbPassengers = vehicleNbPassengers.getOrDefault(vehicleId, 0);
        
        if (currentNbPassengers > 0){
            TransitTripInfo lineInfo = this.vehicleToLineRoute.get(vehicleId);
            if (lineInfo == null) {
                return;
            }
            LinkBinKey currentKey   = new LinkBinKey(timeBin, event.getLinkId(), lineInfo.lineId, lineInfo.lineName, lineInfo.routeId, lineInfo.routeDirection, lineInfo.routeMainDirection);

            int updatedCount = linkNbPassengers.getOrDefault(currentKey, 0) + currentNbPassengers;
            linkNbPassengers.put(currentKey, updatedCount);
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Vehicle> vehicleId = Id.create(event.getVehicleId(), Vehicle.class);
        Id<Person> personId   = event.getPersonId();
        if (!(personId.toString().contains("pt_"))) {
            vehicleNbPassengers.put(vehicleId, vehicleNbPassengers.getOrDefault(vehicleId, 0) - 1);
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Vehicle> vehicleId = Id.create(event.getVehicleId(), Vehicle.class);
        Id<Person> personId   = event.getPersonId();
        if (!(personId.toString().contains("pt_"))) {
            vehicleNbPassengers.put(vehicleId, vehicleNbPassengers.getOrDefault(vehicleId, 0) + 1);
        }
    }

    private String formatTime(int seconds) {
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        return String.format("%02d:%02d", hours, minutes);
    }

    public void writeCSV(String filePath) throws IOException {
        boolean isGzipped = filePath.endsWith(".gz");

        OutputStream outputStream = isGzipped
            ? new GZIPOutputStream(new FileOutputStream(filePath))
            : new FileOutputStream(filePath);

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream))) {
            writer.write("timeBin;linkId;lineId;lineName;routeId;route_direction;line_main_direction;passengers\n");

            List<Map.Entry<LinkBinKey, Integer>> sorted = new ArrayList<>(linkNbPassengers.entrySet());
            sorted.sort(Comparator.comparing(e -> e.getKey().timeBin));

            for (Map.Entry<LinkBinKey, Integer> entry : sorted) {
                LinkBinKey key = entry.getKey();
                int count = entry.getValue();

                String formattedTime = formatTime(key.timeBin * BIN_SIZE);

                writer.write(String.format(
                    "%s;%s;%s;%s;%s;%s;%s;%d\n",
                    formattedTime,
                    key.linkId.toString(),
                    key.lineId,
                    key.lineName,
                    key.routeId,
                    key.routeDirection,
                    key.lineMainDirection,
                    count
                ));
            }
        }
    }
    
}
