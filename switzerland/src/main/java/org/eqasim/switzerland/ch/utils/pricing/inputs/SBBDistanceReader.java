package org.eqasim.switzerland.ch.utils.pricing.inputs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.matsim.pt.transitSchedule.api.TransitSchedule;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class SBBDistanceReader {

    public static Zone createZone(File csvFile) throws CsvValidationException, IOException {
        // 1. Create an authority and a zone
        String authorityName = "SBB";
        String zoneName      = "SBB";

        Authority SBBauthority = new Authority(authorityName, 0, "DistanceBased");

        Zone.Builder sbbZoneBuilder = new Zone.Builder(SBBauthority, zoneName);

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();

            // Find column indexes
            int fromIdIndex = -1;
            int toIdIndex   = -1;

            for (int i = 0; i < headers.length; i++) {
                String col = headers[i].trim().toLowerCase();
                if (col.equals("origin_id")) fromIdIndex = i;
                else if (col.equals("destination_id")) toIdIndex = i;
            }

            if (fromIdIndex == -1 || toIdIndex == -1) {
                throw new IllegalArgumentException("Missing required columns: origin_id, destination_id");
            }

            String[] line;
            while ((line = reader.readNext()) != null) {    
                String originId      = line[fromIdIndex].trim();
                String destinationId = line[toIdIndex].trim();

                sbbZoneBuilder.addStopId(originId);
                sbbZoneBuilder.addStopId(destinationId);   

            }
        }

        Zone sbbZone = sbbZoneBuilder.build();

        return sbbZone;
    }

    public static ZonalRegistry createZonalRegistry(Zone sbbZone) {

        Collection<Authority> authorities = Collections.singleton(sbbZone.getAuthority());
        Collection<Zone> zones            = Collections.singleton(sbbZone);

        return new ZonalRegistry(authorities, zones);
    }
    
}
