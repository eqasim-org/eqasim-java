package org.eqasim.switzerland.ch_cmdp.mode_choice.costs.pt.inputs;

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
    /**
     * The optional column with the priority of the network of a row (empty: the usual one). eqasim-switzerland gives
     * the French local networks (STAR'T, EVA'D) priority 3, above Unireso, whose zones abroad share their stops.
     */
    public static final String PRIORITY_COLUMN = "network priority";
    /** The optional column with whether a stop is in the area of the Swiss fares (true / false; empty: unknown). */
    public static final String LOCATION_COLUMN = "in switzerland";

    /**
     * Where the stops are: {stop id: whether it is in the area of the Swiss fares}, from the 'in switzerland' column
     * (eqasim-switzerland locates the feed stops with the Swiss border); null without the column.
     */
    public Map<String, Boolean> readStopLocations(File csvFile) throws IOException, CsvValidationException {
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();
            int stopIdIndex = -1;
            int locationIndex = -1;
            for (int i = 0; i < headers.length; i++) {
                String col = headers[i].trim();
                if (col.equalsIgnoreCase("stop_id")) stopIdIndex = i;
                else if (col.equalsIgnoreCase(LOCATION_COLUMN)) locationIndex = i;
            }
            if (stopIdIndex == -1 || locationIndex == -1) {
                return null;
            }

            Map<String, Boolean> locations = new HashMap<>();
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length > Math.max(stopIdIndex, locationIndex)) {
                    String value = line[locationIndex].trim();
                    if (!value.isEmpty()) {
                        locations.put(line[stopIdIndex].trim(), Boolean.parseBoolean(value));
                    }
                }
            }
            return locations;
        }
    }

    /**
     * The networks of the 'tarif network' column: priority 1 for a Z-Pass, 2 for the others, or the highest one
     * given in the 'network priority' column of their rows (as Pricing._read_zones in eqasim-switzerland).
     */
    public Collection<Authority> readTarifNetworks(File csvFile) throws IOException, CsvValidationException {
        Map<String, Integer> priorities = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            String[] headers = reader.readNext();
            int index = -1;
            int priorityIndex = -1;

            // Find index of "tarif network" (and of the optional "network priority")
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("tarif network")) {
                    index = i;
                } else if (headers[i].trim().equalsIgnoreCase(PRIORITY_COLUMN)) {
                    priorityIndex = i;
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
                    String given = priorityIndex >= 0 && line.length > priorityIndex ? line[priorityIndex].trim() : "";

                    for (String authID : authorityIds){
                        if (!authID.isEmpty() & !authID.equals("nan")){
                            int priority = priorities.getOrDefault(authID, authID.contains("ZPass") ? 1 : 2);
                            if (!given.isEmpty()) {
                                priority = Math.max(priority, Integer.parseInt(given));
                            }
                            priorities.put(authID, priority);
                        }
                    }
                }
            }
        }

        List<Authority> authorities = new ArrayList<>();
        priorities.forEach((id, priority) -> authorities.add(new Authority(id, priority, "ZoneBased")));
        return authorities;
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