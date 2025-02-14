package org.eqasim.core.simulation.policies.impl.mobility_coins;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.core.utils.io.IOUtils;

public class MobilityCoinsWriter {
    private final File outputPath;
    private final List<Entry> entries = new LinkedList<>();

    public MobilityCoinsWriter(File outputPath) {
        this.outputPath = outputPath;
    }

    public void write(Entry entry) {
        try {
            BufferedWriter writer = IOUtils.getBufferedWriter(outputPath.toString());

            if (entries.size() == 0) {
                writer.write(String.join(";", new String[] {
                        "iteration", //
                        "calculated_market_price", //
                        "smoothed_market_price"
                }) + "\n");
            }

            entries.add(entry);

            for (Entry e : entries) {
                writer.write(String.join(";", new String[] {
                        String.valueOf(e.iteration), //
                        String.valueOf(e.calculatedMarketPrice), //
                        String.valueOf(e.smoothedMarketPrice), //
                }) + "\n");
            }

            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public record Entry(int iteration, double calculatedMarketPrice, double smoothedMarketPrice) {
    }
}
