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
 * Preferred CSV format: linkCategory;isUrban;specialRegion;penalty(%)
 */
public class PenaltyCsvHandler {
    private static final Logger logger = LogManager.getLogger(PenaltyCsvHandler.class);

    /**
     * Reads penalties from a CSV file and populates the provided map.
     * @param filename Path to the CSV file.
     * @param penalties Map to store the read penalties (group -> penalty).
     */
    public static void readPenaltiesFromFile(String filename, Map<PenaltyGroupKey, Double> penalties) {
        try (BufferedReader reader = getBufferedReader(filename)) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 4) {
                    int linkCategory = Integer.parseInt(parts[0].trim());
                    boolean isUrban = Boolean.parseBoolean(parts[1].trim());
                    int specialRegion = Integer.parseInt(parts[2].trim());
                    double penalty = Double.parseDouble(parts[3].trim());
                    penalties.put(new PenaltyGroupKey(linkCategory, isUrban, specialRegion), penalty);
                } else {
                    throw new IllegalArgumentException("Invalid line format in penalties file: " + line);
                }
            }
            logger.info("Read link penalty groups from file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error reading link penalty groups from file: " + filename, e);
        }
    }

    public static Map<PenaltyGroupKey, Double> readPenaltiesFromFile(String filename) {
        Map<PenaltyGroupKey, Double> penalties = new HashMap<>();
        readPenaltiesFromFile(filename, penalties);
        return penalties;
    }

    /**
     * Writes penalties to a CSV file.
     * @param filename Path to the CSV file.
     * @param penalties Map of penalties to write (group -> penalty).
     */
    public static void writePenaltiesToFile(String filename, Map<PenaltyGroupKey, Double> penalties) {
        try (BufferedWriter writer = getBufferedWriter(filename)) {
            writer.write("linkCategory;isUrban;specialRegion;penalty(%)\n");
            for (Map.Entry<PenaltyGroupKey, Double> entry : penalties.entrySet()) {
                writer.write(entry.getKey().getLinkCategory() + ";"
                        + entry.getKey().isUrban() + ";"
                        + entry.getKey().getSpecialRegion() + ";"
                        + String.format("%.4f", entry.getValue()) + "\n");
            }
            logger.info("Wrote link penalty groups to file: " + filename);
        } catch (Exception e) {
            throw new RuntimeException("Error writing link penalty groups to file: " + filename, e);
        }
    }
}
