package org.eqasim.switzerland.ch.utils.pt;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;

import java.io.IOException;

public class PTLinkVolumesWriter implements IterationEndsListener {

    private final PTLinkVolumesHandler handler;
    private final String filename;

    public PTLinkVolumesWriter(PTLinkVolumesHandler handler, String outputPath) {
        this.handler = handler;
        this.filename = outputPath;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int current = event.getIteration();
        int last = event.getServices().getConfig().controller().getLastIteration();
        if (current == last) {
            try {
                String fullPath = event.getServices().getControlerIO().getOutputFilename(filename);
                handler.writeCSV(fullPath);
                System.out.println("✔ PT link volumes written to: " + fullPath);
            } catch (IOException e) {
                throw new RuntimeException("❌ Failed to write PT passenger count CSV", e);
            }
        }
    }
    
}
