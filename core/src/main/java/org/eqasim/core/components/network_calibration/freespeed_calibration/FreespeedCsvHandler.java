package org.eqasim.core.components.network_calibration.freespeed_calibration;

import org.apache.logging.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FreespeedCsvHandler {
    public static Map<LinkGroupKey, Double> readFactors(String filename) {
        Map<LinkGroupKey, Double> factors = new HashMap<>();

        try (BufferedReader reader = IOUtils.getBufferedReader(filename)) {
            String line = reader.readLine(); // header
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length < 3) {
                    continue;
                }

                int category = Integer.parseInt(parts[0].trim());
                String municipalityType = parts[1].trim();
                double factor = Double.parseDouble(parts[2].trim());
                factors.put(new LinkGroupKey(category, municipalityType), factor);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading freespeed factors from file: " + filename, e);
        }

        return factors;
    }

    public static void writeFactors(String filename, Map<LinkGroupKey, Double> factors) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write("category;municipalityType;factor\n");
            for (Map.Entry<LinkGroupKey, Double> entry : factors.entrySet()) {
                LinkGroupKey key = entry.getKey();
                double val = round(entry.getValue());
                writer.write(key.getCategory() + ";" + key.getMunicipalityType() + ";" + val + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing freespeed factors to file: " + filename, e);
        }
    }

    public static void writeGroupStats(String filename, Map<LinkGroupKey, FreespeedFactorManager.GroupStats> groupStats) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write("category;municipalityType;tripCount;simulatedTime_s;observedTime_s;timeRatio;statsError;statsFactor\n");
            for (Map.Entry<LinkGroupKey, FreespeedFactorManager.GroupStats> entry : groupStats.entrySet()) {
                LinkGroupKey key = entry.getKey();
                FreespeedFactorManager.GroupStats stats = entry.getValue();
                double ratio = stats.observedTime > 0.0 ? stats.simulatedTime / stats.observedTime : Double.NaN;
                double error = stats.getAverageErrors();
                double proposedFactor = stats.getAverageFactor();
                writer.write(key.getCategory() + ";" + key.getMunicipalityType() + ";" + stats.tripCount + ";"
                        + round(stats.simulatedTime) + ";" + round(stats.observedTime) +";" + round(ratio) + ";"
                        + error + ";" + proposedFactor + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing freespeed group stats to file: " + filename, e);
        }
    }

    public static void writeDiagnostics(String filename, Map<LinkGroupKey, FreespeedFactorManager.GroupDiagnostics> diagnostics) {
        try (BufferedWriter writer = IOUtils.getBufferedWriter(filename)) {
            writer.write("category;municipalityType;frozen;noImprovementStreak;decisions;factors;errors\n");

            for (Map.Entry<LinkGroupKey, FreespeedFactorManager.GroupDiagnostics> entry : diagnostics.entrySet()) {
                LinkGroupKey key = entry.getKey();
                FreespeedFactorManager.GroupDiagnostics d = entry.getValue();

               writer.write(key.getCategory() + ";" + key.getMunicipalityType() + ";"
                       + d.frozen + ";"
                       + d.noImprovementStreak + ";"
                       + joinStringList(d.decisions.toList().stream().map(String::valueOf).toList()) + ";"
                       + joinList(d.lastFactors.toList()) + ";"
                       + joinList(d.lastErrors.toList()) + "\n");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing freespeed diagnostics to file: " + filename, e);
        }
    }

    public static double round(double x){
        return (Double.isNaN(x) || Double.isInfinite(x)) ? x : Math.round(x*100.0)/100.0;
    }

    private static String joinList(List<Double> values) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                buffer.append(',');
            }
            buffer.append(round(values.get(i)));
        }
        return buffer.toString();
    }

    private static String joinStringList(List<String> values) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                buffer.append(',');
            }
            buffer.append(values.get(i));
        }
        return buffer.toString();
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

