package org.eqasim.examples.zurich_parking.analysis.parking;

import com.google.inject.Inject;
import org.matsim.contrib.parking.parkingsearch.manager.ParkingSearchManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.IOException;

public class ParkingOccupancyListener implements IterationEndsListener {

    @Inject
    ParkingSearchManager parkingSearchManager;
    @Inject
    OutputDirectoryHierarchy output;

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();

        // write out stats
        ParkingOccupancyStatsWriter writer = new ParkingOccupancyStatsWriter(this.parkingSearchManager.produceStatistics());
        String outputPath = output.getIterationFilename(iteration, "parking_occupancy_stats.csv");

        try {
            writer.write(outputPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.parkingSearchManager.reset(iteration);
    }
}
