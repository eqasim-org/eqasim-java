package org.eqasim.examples.zurich_parking.analysis.parking;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;

public class ParkingOccupancyStatsWriter {
    final private Collection<ParkingOccupancyStats> items;
    final private String delimiter;

    public ParkingOccupancyStatsWriter(Collection<ParkingOccupancyStats> items) {
        this(items, ";");
    }

    public ParkingOccupancyStatsWriter(Collection<ParkingOccupancyStats> items, String delimiter) {
        this.items = items;
        this.delimiter = delimiter;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (ParkingOccupancyStats item : items) {
            writer.write(formatTrip(item) + "\n");
            writer.flush();
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(delimiter, new String[] { //
                "time", //
                "link_id", //
                "from_x",
                "from_y",
                "to_x",
                "to_y",
                "parking_facility_id", //
                "parking_facility_type", //
                "parking_x", //
                "parking_y", //
                "occupancy", //
                "capacity" //
        });
    }

    private String formatTrip(ParkingOccupancyStats item) {
        return String.join(delimiter, new String[] { //
                String.valueOf(item.time), //
                item.linkId.toString(), //
                String.valueOf(item.fromCoord.getX()), //
                String.valueOf(item.fromCoord.getY()), //
                String.valueOf(item.toCoord.getX()), //
                String.valueOf(item.toCoord.getY()), //
                item.parkingFacilityId.toString(), //
                item.parkingFacilityType, //
                String.valueOf(item.parkingFacilityCoord.getX()), //
                String.valueOf(item.parkingFacilityCoord.getY()), //
                String.valueOf(item.occupancy), //
                String.valueOf(item.capacity)
        });
    }
}
