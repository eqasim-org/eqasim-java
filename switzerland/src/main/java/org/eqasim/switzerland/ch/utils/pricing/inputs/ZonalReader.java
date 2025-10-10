package org.eqasim.switzerland.ch.utils.pricing.inputs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

public class ZonalReader {

    public Collection<Authority> readTarifNetworks(File csvFile) throws IOException, CsvValidationException {
        Map<String, Authority> authorities = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();
            int index = -1;

            // Find index of "tarif network"
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("tarif network")) {
                    index = i;
                    break;
                }
            }

            if (index == -1) {
                throw new IllegalArgumentException("CSV does not contain a 'tarif network' column.");
            }

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length > index) {
                    String authority      = line[index].trim().replaceAll("[\\[\\]' ]", "");
                    String[] authorityIds = authority.split(",");
                    
                    for (String authID : authorityIds){
                        if (!authID.isEmpty() & !authID.equals("nan")){
                            if (authID.contains("ZPass")){
                                authorities.putIfAbsent(authID, new Authority(authID, 1, "ZoneBased"));
                            }
                            else{
                                authorities.putIfAbsent(authID, new Authority(authID, 2, "ZoneBased"));
                            }
                        }
                    }
                }
            }
        }

        return authorities.values();
    }

    public Collection<Zone> readZones(File csvFile, Collection<Authority> authorities) throws CsvValidationException, IOException {
        // Map authorities by their ID
        Map<String, Authority> authoritiesMap = new HashMap<>();
        for (Authority authority : authorities) {
            authoritiesMap.put(authority.getId(), authority);
        }

        // Zone builders grouped by authority and zoneId
        Map<Authority, Map<String, Zone.Builder>> builders = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();

            // Find column indexes
            int stopIdIndex = -1;
            int authorityIdIndex = -1;
            int zoneIdIndex = -1;

            for (int i = 0; i < headers.length; i++) {
                String col = headers[i].trim().toLowerCase();
                if (col.equals("stop_id")) stopIdIndex = i;
                else if (col.equals("tarif network")) authorityIdIndex = i;
                else if (col.equals("zones")) zoneIdIndex = i;
            }

            if (stopIdIndex == -1 || authorityIdIndex == -1 || zoneIdIndex == -1) {
                throw new IllegalArgumentException("Missing required columns: stop_id, tarif network, zones");
            }

            String[] line;
            while ((line = reader.readNext()) != null) {                
                // stop ID
                String stopIdStr   = line[stopIdIndex].trim();

                // zones
                String[] zoneEntries = line[zoneIdIndex].split(",");

                // add builders
                for (String entry : zoneEntries){
                    String[] parts = entry.trim().split(":");
                    if (parts.length == 2){
                        String authorityID = parts[0].trim();
                        String zoneID      = parts[1].trim();

                        Authority authority = authoritiesMap.get(authorityID);
                        if (authority == null) continue;

                        builders.computeIfAbsent(authority, k -> new HashMap<>()).computeIfAbsent(zoneID, z -> new Zone.Builder(authority, zoneID)).addStopId(stopIdStr);
                    }
                }

            }
        }

        // Build final zone list
        List<Zone> zones = new ArrayList<>();
        for (Map<String, Zone.Builder> authorityBuilders : builders.values()) {
            for (Zone.Builder builder : authorityBuilders.values()) {
                zones.add(builder.build());
            }
        }

        return zones;
    }
    
}