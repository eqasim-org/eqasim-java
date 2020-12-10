package org.eqasim.switzerland.parking;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class ParkingInfoReader {

    public Map<Id<Person>,List<ParkingInfo>> read(String path, String delimiter) throws IOException {

        Map<Id<Person>,List<ParkingInfo>> parkingInfoMap = new HashMap<>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)));

        List<String> header = null;
        String line = null;

        while ((line = reader.readLine()) != null) {
            List<String> row = Arrays.asList(line.split(delimiter));

            if (header == null) {
                header = row;
            } else {

                // read parking info from csv
                Id<Person> personId = Id.createPersonId(row.get(header.indexOf("person_id")));
                int tripId = Integer.parseInt(row.get(header.indexOf("person_trip_id")));
                double parkingCost = Double.parseDouble(row.get(header.indexOf("parking_cost")));
                double populationDensity = Double.parseDouble(row.get(header.indexOf("population_density")));

                // add parking info to map
                parkingInfoMap.putIfAbsent(personId, new LinkedList<>());
                parkingInfoMap.get(personId).add(tripId, new ParkingInfo(parkingCost, populationDensity));

            }

        }

        return parkingInfoMap;
    }
}

