package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.util.Map;

public class FreespeedCsvHandler {
    public static void writeFactors(String filename, Map<LinkGroupKey, Double> factors) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write("category;municipalityType;factor\n");
            for (Map.Entry<LinkGroupKey, Double> entry : factors.entrySet()) {
                LinkGroupKey key = entry.getKey();
                writer.write(key.getCategory() + ";" + key.getMunicipalityType() + ";" + entry.getValue() + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing freespeed factors to file: " + filename, e);
        }
    }

    public static void writeGroupStats(String filename, Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write("category;municipalityType;tripCount;simulatedTime_s;observedTime_s;simulatedDistance_m;observedDistance_m;timeRatio\n");
            for (Map.Entry<LinkGroupKey, FreespeedFactorManager.GroupStats> entry : groupStats.entrySet()) {
                LinkGroupKey key = entry.getKey();
                FreespeedFactorManager.GroupStats stats = entry.getValue();
                double ratio = stats.observedTime > 0.0 ? stats.simulatedTime / stats.observedTime : Double.NaN;
                writer.write(key.getCategory() + ";" + key.getMunicipalityType() + ";" + stats.tripCount + ";"
                        + stats.simulatedTime + ";" + stats.observedTime + ";" + stats.simulatedDistance + ";"
                        + stats.observedDistance + ";" + ratio + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing freespeed group stats to file: " + filename, e);
        }
    }

    public static void logFactorsByMunicipalityType(Logger logger, Map<LinkGroupKey, Double> factors) {
        if (factors == null || factors.isEmpty()) {
            logger.info("No freespeed factors available.");
            return;
        }

        Map<String, Map<String, Double>> grouped = new java.util.TreeMap<>();

        for (Map.Entry<LinkGroupKey, Double> entry : factors.entrySet()) {
            LinkGroupKey key = entry.getKey();
            String municipalityType = String.valueOf(key.getMunicipalityType());
            String category = String.valueOf(key.getCategory());

            grouped.computeIfAbsent(municipalityType, k -> new java.util.TreeMap<>())
                    .put(category, entry.getValue());
        }

        logger.info("Freespeed factors by municipality type:");
        for (Map.Entry<String, Map<String, Double>> municipalityEntry : grouped.entrySet()) {
            StringBuilder line = new StringBuilder();
            String mun = String.format("%-10s", municipalityEntry.getKey());
            line.append("\t \t ").append(mun).append(": ");

            boolean first = true;
            for (Map.Entry<String, Double> categoryEntry : municipalityEntry.getValue().entrySet()) {
                if (!first) {
                    line.append(",  ");
                }
                double f = Math.round(categoryEntry.getValue()*100.0)/100.0;
                line.append(categoryEntry.getKey()).append(" = ").append(f);
                first = false;
            }

            logger.info(line.toString());
        }
    }
}

