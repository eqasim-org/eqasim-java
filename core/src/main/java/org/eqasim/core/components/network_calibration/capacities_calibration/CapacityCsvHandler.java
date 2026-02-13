package org.eqasim.core.components.network_calibration.capacities_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.Map;

import static org.matsim.core.utils.io.IOUtils.getBufferedReader;
import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

/**
 * Handles reading and writing of capacity data to/from CSV files.
 * The CSV format is expected to have columns: Category;Capacity(veh/h/lane)
 */
public class CapacityCsvHandler {
    private static final Logger logger = LogManager.getLogger(CapacityCsvHandler.class);

    /**
     * Reads capacities from a CSV file and populates the provided map.
     * @param filename Path to the CSV file.
     * @param capacities Map to store the read capacities (category -> capacity).
     */
    public static void readCapacitiesFromFile(String filename, Map<Integer, Double> capacities) {
        try (BufferedReader reader = getBufferedReader(filename)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    int category = Integer.parseInt(parts[0]);
                    double capacity = Double.parseDouble(parts[1]);
                    capacities.put(category, capacity);
                }
            }
            logger.info("Read link category capacities from file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error reading link category capacities from file: " + filename, e);
        }
    }

    /**
     * Writes capacities to a CSV file.
     * @param filename Path to the CSV file.
     * @param capacities Map of capacities to write (category -> capacity).
     */
    public static void writeCapacitiesToFile(String filename, Map<Integer, Double> capacities) {
        try (BufferedWriter writer = getBufferedWriter(filename)) {
            writer.write("Category;Capacity(veh/h/lane)\n");
            for (Map.Entry<Integer, Double> entry : capacities.entrySet()) {
                writer.write(entry.getKey() + ";" + String.format("%.4f", entry.getValue()) + "\n");
            }
            logger.info("Wrote link category capacities to file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error writing link category capacities to file: " + filename, e);
        }
    }
}