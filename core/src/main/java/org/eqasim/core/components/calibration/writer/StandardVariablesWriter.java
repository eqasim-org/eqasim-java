package org.eqasim.core.components.calibration.writer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.calibration.VariablesWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


public class StandardVariablesWriter implements VariablesWriter {
    protected final Logger logger = LogManager.getLogger(StandardVariablesWriter.class);

    protected final Object LOCK;
    protected BufferedWriter ptWriter = null;
    protected BufferedWriter carWriter = null;
    protected BufferedWriter bikeWriter = null;
    protected BufferedWriter walkWriter = null;
    protected BufferedWriter cpWriter = null;
    protected final AtomicInteger selectionCounter;
    protected String csvFilePath = null;
    protected boolean initiated = false;

    public StandardVariablesWriter() {
        LOCK = new Object();
        selectionCounter = new AtomicInteger(0);
    }

    public void init(String filePath) {
        if (filePath == null) {
            return;
        }

        synchronized (LOCK) {
            if (filePath.equals(csvFilePath) && initiated) {return;} // if it is the same file, do nothing.
            if (initiated) {
                close(); // close the previous files if are initiated.
            }
            try {
                initWriters(filePath);
                csvFilePath = filePath;
                initiated = true;
            } catch (IOException e) {
                throw new RuntimeException("EstimatorLogger: Failed to initialize CSV writer", e);
            }
        }
    }

    public void close() {
        try {
            if (ptWriter != null) ptWriter.close();
            if (carWriter != null) carWriter.close();
            if (walkWriter != null) walkWriter.close();
            if (bikeWriter != null) bikeWriter.close();
            if (cpWriter != null) cpWriter.close();
        } catch (IOException e) {
            logger.error("Failed to close writer: {}", e.getMessage());
        }
        initiated = false;
        logger.info("Variables writer closed.");
    }

    public boolean isInitiated() {return initiated;}

    protected void initWriters(String basePath) throws IOException {
        // Extract parent directory and base filename
        Path baseDir = Path.of(basePath).getParent();
        String baseName = Path.of(basePath).getFileName().toString();

        // Create writers with appropriate filenames and headers
        ptWriter = newBufferedWriterForMode(baseDir, baseName, "pt");
        writeHeader(ptWriter, "pt");

        carWriter = newBufferedWriterForMode(baseDir, baseName, "car");
        writeHeader(carWriter, "car");

        bikeWriter = newBufferedWriterForMode(baseDir, baseName, "bike");
        writeHeader(bikeWriter, "bike");

        walkWriter = newBufferedWriterForMode(baseDir, baseName, "walk");
        writeHeader(walkWriter, "walk");

        cpWriter = newBufferedWriterForMode(baseDir, baseName, "car_passenger");
        writeHeader(cpWriter, "car_passenger");

        logger.info("Variables writer initialized for base path: {}", basePath);
    }

    protected BufferedWriter newBufferedWriterForMode(Path baseDir, String baseName, String mode) throws IOException {
        String newFilename = baseName.replace(".csv", "_" + mode + ".csv");
        Path fullPath = baseDir == null ? Path.of(newFilename) : baseDir.resolve(newFilename);
        return Files.newBufferedWriter(fullPath);
    }

    protected void writeHeader(BufferedWriter writer, String mode) {
        try {
            switch (mode) {
                case "pt" -> writer.write("person_id;trip_index;departure_time;utility;accessEgressTime_min;inVehicleTime_min;waitingTime_min;numberOfLineSwitches;cost_MU;euclideanDistance_km\n");
                case "car" -> writer.write("person_id;trip_index;departure_time;utility;travelTime_min;accessEgressTime_min;euclideanDistance_km;cost_MU;statedPreferenceRegion\n");
                case "bike" -> writer.write("person_id;trip_index;departure_time;utility;statedPreferenceRegion;travelTime_min;age_a;euclideanDistance_km\n");
                case "walk" -> writer.write("person_id;trip_index;departure_time;utility;travelTime_min;euclideanDistance_km\n");
                case "car_passenger" -> writer.write("person_id;trip_index;departure_time;utility;euclideanDistance_km\n");
                default -> System.err.println("Unknown mode: " + mode);
            }
            writer.flush();
        } catch (IOException e) {
            System.err.println("Failed to write the header for mode \"" + mode + "\": " + e.getMessage());
        }
    }

    public void writeVariables(String mode, String personId, int tripIndex, double departureTime,
                                      double utility, Map<String, String> attributes){
        if (!initiated) {return;}

        synchronized (LOCK) {
            try {
                switch (mode) {
                    case "pt":
                        ptWriter.write(String.format("%s;%d;%d;%.2f;%s;%s;%s;%s;%s;%s\n",
                                personId,
                                tripIndex,
                                (int) departureTime,
                                utility,
                                attributes.get("accessEgressTime_min"),
                                attributes.get("inVehicleTime_min"),
                                attributes.get("waitingTime_min"),
                                attributes.get("numberOfLineSwitches"),
                                attributes.get("cost_MU"),
                                attributes.get("euclideanDistance_km")
                        ));
                        ptWriter.flush();
                        break;

                    case "car":
                        carWriter.write(String.format("%s;%d;%d;%.2f;%s;%s;%s;%s;%s\n",
                                personId,
                                tripIndex,
                                (int) departureTime,
                                utility,
                                attributes.get("travelTime_min"),
                                attributes.get("accessEgressTime_min"),
                                attributes.get("euclideanDistance_km"),
                                attributes.get("cost_MU"),
                                attributes.get("statedPreferenceRegion")
                        ));
                        carWriter.flush();
                        break;

                    case "bike":
                        bikeWriter.write(String.format("%s;%d;%d;%.2f;%s;%s;%s;%s\n",
                                personId,
                                tripIndex,
                                (int) departureTime,
                                utility,
                                attributes.get("statedPreferenceRegion"),
                                attributes.get("travelTime_min"),
                                attributes.get("age_a"),
                                attributes.get("euclideanDistance_km")
                        ));
                        bikeWriter.flush();
                        break;

                    case "walk":
                        walkWriter.write(String.format("%s;%d;%d;%.2f;%s;%s\n",
                                personId,
                                tripIndex,
                                (int) departureTime,
                                utility,
                                attributes.get("travelTime_min"),
                                attributes.get("euclideanDistance_km")
                        ));
                        walkWriter.flush();
                        break;

                    case "car_passenger":
                        cpWriter.write(String.format("%s;%d;%d;%.2f;%s\n",
                                personId,
                                tripIndex,
                                (int) departureTime,
                                utility,
                                attributes.get("euclideanDistance_km")
                        ));
                        cpWriter.flush();
                        break;
                    default :
                        System.err.println("Unknown mode: " + mode);
                }

            } catch (IOException e) {
                System.err.println("Failed to write to CSV: " + e.getMessage());
            }
        }
    }

}