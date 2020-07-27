package org.eqasim.san_francisco.bike.analysis.counts.writers;

import org.eqasim.san_francisco.bike.analysis.counts.items.CountItem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

public class BikeCountWriter {
    private final Map<Id<Link>, List<CountItem>> items;

    public BikeCountWriter(Map<Id<Link>, List<CountItem>> items) {
        this.items = items;
    }

    public void write(String outputPath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

        writer.write(formatHeader() + "\n");
        writer.flush();

        for (Id<Link> linkId : items.keySet()) {
            for (CountItem item : items.get(linkId)) {
                writer.write(formatItem(item) + "\n");
                writer.flush();
            }
        }

        writer.flush();
        writer.close();
    }

    private String formatHeader() {
        return String.join(";", new String[] {
                "linkId", "mode", "time", "count"
        });
    }

    private String formatItem(CountItem item) {
        return String.join(";", new String[] {
                item.linkId.toString(),
                item.mode,
                String.valueOf(item.time),
                String.valueOf(item.count)
        });
    }
}
