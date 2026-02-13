package org.eqasim.core.components.network_calibration.cost_calibration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.Map;

import static org.matsim.core.utils.io.IOUtils.getBufferedReader;
import static org.matsim.core.utils.io.IOUtils.getBufferedWriter;

/**
 * Handles reading and writing of penalty data to/from CSV files.
 * The CSV format is expected to have columns: category;penalty(%)
 */
public class PenaltyCsvHandler {
    private static final Logger logger = LogManager.getLogger(PenaltyCsvHandler.class);

    /**
     * Reads penalties from a CSV file and populates the provided map.
     * @param filename Path to the CSV file.
     * @param penalties Map to store the read penalties (category -> penalty).
     */
    public static void readPenaltiesFromFile(String filename, Map<Integer, Double> penalties) {
        try (BufferedReader reader = getBufferedReader(filename)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    int category = Integer.parseInt(parts[0]);
                    double penalty = Double.parseDouble(parts[1]);
                    penalties.put(category, penalty);
                }
            }
            logger.info("Read link category penalties from file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error reading link category penalties from file: " + filename, e);
        }
    }

    public static Map<Integer, Double> readPenaltiesFromFile(String filename) {
        Map<Integer, Double> penalties = new HashMap<>();
        try (BufferedReader reader = getBufferedReader(filename)) {
            String line = reader.readLine(); // skip header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    int category = Integer.parseInt(parts[0]);
                    double penalty = Double.parseDouble(parts[1]);
                    penalties.put(category, penalty);
                }
            }
            logger.info("Read link category penalties from file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error reading link category penalties from file: " + filename, e);
        }
        return penalties;
    }

    /**
     * Writes penalties to a CSV file.
     * @param filename Path to the CSV file.
     * @param penalties Map of penalties to write (category -> penalty).
     */
    public static void writePenaltiesToFile(String filename, Map<Integer, Double> penalties) {
        try (BufferedWriter writer = getBufferedWriter(filename)) {
            writer.write("category;penalty(%)\n");
            for (Map.Entry<Integer, Double> entry : penalties.entrySet()) {
                writer.write(entry.getKey() + ";" + String.format("%.4f", entry.getValue()) + "\n");
            }
            logger.info("Wrote link category penalties to file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error writing link category penalties to file: " + filename, e);
        }
    }
}
