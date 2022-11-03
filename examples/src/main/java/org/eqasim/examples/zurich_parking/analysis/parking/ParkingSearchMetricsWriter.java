package org.eqasim.examples.zurich_parking.analysis.parking;

import java.io.*;
import java.util.Collection;

public class ParkingSearchMetricsWriter {
    final private Collection<ParkingSearchItem> items;
    final private String delimiter;

    public ParkingSearchMetricsWriter(Collection<ParkingSearchItem> items) {
        this(items, ";");
    }

    public ParkingSearchMetricsWriter(Collection<ParkingSearchItem> items, String delimiter) {
        this.items = items;
        this.delimiter = delimiter;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (ParkingSearchItem item : items) {
            writer.write(formatTrip(item) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(delimiter, new String[] { //
                "person_id", //
                "trip_id", //
                "trip_purpose", //
                "destination_x", //
                "destination_y", //
                "parking_search_start_time",
                "parking_search_end_time",
                "arrival_time", //
                "parking_facility_id", //
                "parking_facility_type",
                "parking_x",
                "parking_y",
                "parking_search_time", //
                "parking_search_distance", //
                "egress_walk_time", //
                "egress_walk_distance", //
                "duplicated_distance" //
        });
    }

    private String formatTrip(ParkingSearchItem item) {
        return String.join(delimiter, new String[] { //
                String.valueOf(item.personId), //
                String.valueOf(item.tripId), //
                item.tripPurpose, //
                item.destinationCoord == null ? "NaN" : String.valueOf(item.destinationCoord.getX()), //
                item.destinationCoord == null ? "NaN" : String.valueOf(item.destinationCoord.getY()), //
                String.valueOf(item.parkingSearchStartTime.seconds()), //
                String.valueOf(item.parkingSearchEndTime.seconds()), //
                String.valueOf(item.arrivalTime.seconds()), //
                item.parkingFacilityId.isEmpty() ? "NaN" : item.parkingFacilityId.get().toString(), //
                item.parkingFacilityType.isEmpty() ? "NaN" : item.parkingFacilityType.get(), //
                item.parkingCoord.isEmpty() ? "NaN" : String.valueOf(item.parkingCoord.get().getX()), //
                item.parkingCoord.isEmpty() ? "NaN" : String.valueOf(item.parkingCoord.get().getY()), //
                String.valueOf(item.parkingSearchTime), //
                String.valueOf(item.parkingSearchDistance), //
                String.valueOf(item.egressWalkTime), //
                String.valueOf(item.egressWalkDistance), //
                String.valueOf(item.duplicatedDistance) //
        });
    }
}
