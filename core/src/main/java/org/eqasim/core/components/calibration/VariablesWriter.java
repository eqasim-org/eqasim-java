package org.eqasim.core.components.calibration;

import java.util.Map;

/**
 * Interface for writing calibration variables.
 * Implementations should handle initialization, writing, and closing of writers.
 */
public interface VariablesWriter {
    /**
     * Initializes the writers.
     */
    void init(String filePath);

    /**
     * Closes all writers.
     */
    void close();

    /**
     * Writes the provided mode, person ID, trip index, departure time, utility, and attributes.
     * @param mode The mode of transportation.
     * @param personId The ID of the person.
     * @param tripIndex The index of the trip.
     * @param departureTime The departure time.
     * @param utility The utility value.
     * @param attributes Additional attributes as key-value pairs.
     */
    void writeVariables(String mode, String personId, int tripIndex, double departureTime,
                        double utility, Map<String, String> attributes);

    boolean isInitiated();
}