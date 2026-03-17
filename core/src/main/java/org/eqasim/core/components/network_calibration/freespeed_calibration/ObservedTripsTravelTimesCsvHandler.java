package org.eqasim.core.components.network_calibration.freespeed_calibration;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.databind.MappingIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class ObservedTripsTravelTimesCsvHandler {
    private static final Logger logger = LogManager.getLogger(ObservedTripsTravelTimesCsvHandler.class);

    public static List<ObservedSpeedTrip> readTrips(String filename) {
        try {
            File inputFile = new File(filename);
            if (!inputFile.exists()) {
                throw new IllegalArgumentException("Observed speed trips file does not exist: " + filename);
            }

            CsvMapper mapper = new CsvMapper();
            CsvSchema schema = mapper.typedSchemaFor(ObservedSpeedTrip.class)
                    .withHeader()
                    .withColumnSeparator(',')
                    .withComments()
                    .withColumnReordering(true);

            MappingIterator<ObservedSpeedTrip> iterator = mapper.readerWithTypedSchemaFor(ObservedSpeedTrip.class)
                    .with(schema)
                    .readValues(inputFile);
            List<ObservedSpeedTrip> trips = iterator.readAll();
            logger.info("Read {} observed speed trips from {}", trips.size(), filename);
            return trips;
        } catch (Exception e) {
            throw new RuntimeException("Error reading observed speed trips from file: " + filename, e);
        }
    }

    public static class ObservedSpeedTrip {
        @JsonProperty("identifier")
        @JsonAlias({"Identifier", "ID", "id"})
        public String identifier = "";

        @JsonProperty("departure_x")
        @JsonAlias({"origin_x", "departureX", "originX"})
        public double departureX;

        @JsonProperty("departure_y")
        @JsonAlias({"origin_y", "departureY", "originY"})
        public double departureY;

        @JsonProperty("arrival_x")
        @JsonAlias({"destination_x", "arrivalX", "destinationX"})
        public double arrivalX;

        @JsonProperty("arrival_y")
        @JsonAlias({"destination_y", "arrivalY", "destinationY"})
        public double arrivalY;

        @JsonProperty("departure_time")
        @JsonAlias({"time", "departureTime","departure_time"})
        public double departureTime;

        @JsonProperty("travel_time")
        @JsonAlias({"travelTime", "travel_time"})
        public double travelTimeSeconds;

        @JsonProperty("traveled_distance")
        @JsonAlias({"travel_distance", "distance", "travelDistance"})
        public double traveledDistanceMeters;

        public String getIdentifier(int index) {
            if (identifier != null && !identifier.isBlank()) {
                return identifier;
            }
            return String.valueOf(index);
        }
    }


}