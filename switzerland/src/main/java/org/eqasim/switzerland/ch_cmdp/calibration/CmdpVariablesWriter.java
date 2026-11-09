package org.eqasim.switzerland.ch_cmdp.calibration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eqasim.core.components.calibration.writer.StandardVariablesWriter;
import org.eqasim.core.components.calibration.writer.CalibrationVariableReferences;

/** Exports the inputs needed to reproduce all five CMDP utility functions. */
public class CmdpVariablesWriter extends StandardVariablesWriter {
    @Override
    public void init(String filePath) {
        synchronized (LOCK) {
            if (filePath == null || (initiated && filePath.equals(csvFilePath))) return;
            super.init(filePath);
            CalibrationVariableReferences.start();
        }
    }

    @Override
    public void close() {
        synchronized (LOCK) {
            super.close();
            CalibrationVariableReferences.stop();
        }
    }
    private static final List<String> COMMON = List.of(
            "euclideanDistance_km", "age", "sex", "region", "retired", "junior", "lowIncome", "highIncome",
            "income", "canton", "originHome", "destinationWork", "destinationHome", "destinationEducation",
            "destinationShopping", "destinationLeisure", "destinationOther", "urbanDestination",
            "urbancoreDestination", "shortDistance", "longDistance", "employeeDensity", "companiesDensity",
            "populationDensity", "densitiesAvailable");
    private static final Map<String, List<String>> MODE_COLUMNS = Map.of(
            "car", List.of("carOwnershipRatio", "suburbanDestination", "destinationCity", "carASC",
                    "travelTime_min", "parking_searching_duration_min", "cost_MU", "parking_cost_MU"),
            "pt", List.of("homePtQuality", "destinationPtQuality", "ptRegionalRadius_km",
                    "accessEgressTime_min", "inVehicleTime_min", "waitingTime_min", "numberOfLineSwitches", "cost_MU"),
            "bike", List.of("travelTime_min"), "walk", List.of("travelTime_min"),
            "car_passenger", List.of("drivingLicense", "veryLongDistance", "carOwnershipRatio", "hasCar", "travelTime_min"));
    private static final Map<String, List<String>> COLUMNS = MODE_COLUMNS.entrySet().stream().collect(
            java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> {
                List<String> columns = new ArrayList<>(COMMON);
                columns.addAll(entry.getValue());
                return List.copyOf(columns);
            }));

    private static List<String> columns(String mode) {
        List<String> columns = COLUMNS.get(mode);
        if (columns == null) throw new IllegalArgumentException("Unknown calibration mode: " + mode);
        return columns;
    }

    @Override
    protected void writeHeader(BufferedWriter writer, String mode) {
        try {
            writer.write("person_id;trip_index;departure_time;utility;variables_id;" + String.join(";", columns(mode)));
            writer.newLine();
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot write calibration header for " + mode, e);
        }
    }

    @Override
    public void writeVariables(String mode, String personId, int tripIndex, double departureTime,
            double utility, Map<String, String> attributes) {
        synchronized (LOCK) {
            if (!initiated) return;
            StringBuilder row = new StringBuilder(512);
            row.append(escape(personId)).append(';').append(tripIndex).append(';')
                    .append(departureTime).append(';').append(utility).append(';')
                    .append(CalibrationVariableReferences.record());
            for (String column : columns(mode)) {
                String value = attributes.get(column);
                if (value == null) throw new IllegalArgumentException("Missing " + mode + " calibration variable: " + column);
                row.append(';').append(escape(value));
            }
            BufferedWriter writer = switch (mode) {
                case "car" -> carWriter;
                case "pt" -> ptWriter;
                case "bike" -> bikeWriter;
                case "walk" -> walkWriter;
                case "car_passenger" -> cpWriter;
                default -> throw new IllegalArgumentException("Unknown calibration mode: " + mode);
            };
            try {
                writer.write(row.toString());
                writer.newLine();
                // Buffered until iteration end, rather than flushed for every evaluated alternative.
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot write " + mode + " calibration variables", e);
            }
        }
    }

    private static String escape(String value) {
        return value.indexOf(';') >= 0 || value.indexOf('"') >= 0 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0
                ? "\"" + value.replace("\"", "\"\"") + "\"" : value;
    }
}
