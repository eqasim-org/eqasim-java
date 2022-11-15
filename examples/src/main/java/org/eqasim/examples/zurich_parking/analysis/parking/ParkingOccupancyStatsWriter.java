package org.eqasim.examples.zurich_parking.analysis.parking;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Collection;
import java.util.List;

public class ParkingOccupancyStatsWriter {
    final private Collection<String> strings;

    public ParkingOccupancyStatsWriter(List<String> strings) {
        this.strings = strings;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));
        for (String s : strings){
            writer.write(s);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    public static String formatHeader(String delimiter) {
        return String.join(delimiter, new String[] { //
                "time", //
                "parking_facility_id", //
                "parking_facility_type", //
                "occupancy", //
                "capacity" //
        });
    }

    public static String formatItem(ParkingOccupancyStats item, String delimiter) {
        return String.join(delimiter, new String[] { //
                String.valueOf(item.time), //
                item.parkingFacilityId.toString(), //
                item.parkingFacilityType, //
                String.valueOf(item.occupancy), //
                String.valueOf(item.capacity)
        });
    }
}
