package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

/** Counts passengers at half the configured last iteration and at the last iteration. */
public class PTPassengerAnalyser implements IterationStartsListener, IterationEndsListener {
    private static final Logger logger = LogManager.getLogger(PTPassengerAnalyser.class);
    private static final String PT_FILE_NAME = "pt_passenger_counts.csv.gz";

    private final PTPassengerCountingHandler ptPassengerCountingHandler;
    private boolean analysisActive = false;

    @Inject
    public PTPassengerAnalyser(PTPassengerCountingHandler ptPassengerCountingHandler) {
        this.ptPassengerCountingHandler = ptPassengerCountingHandler;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        int lastIteration = event.getServices().getConfig().controller().getLastIteration();
        if (event.getIteration() == lastIteration / 2 || event.getIteration() == lastIteration) {
            // The handler is detached between analysis iterations, so reset it explicitly.
            ptPassengerCountingHandler.reset(event.getIteration());
            event.getServices().getEvents().addHandler(ptPassengerCountingHandler);
            analysisActive = true;
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!analysisActive) return;
        analysisActive = false;
        event.getServices().getEvents().removeHandler(ptPassengerCountingHandler);
        var output = event.getServices().getControlerIO();
        // Keep the final file at its existing path; retain the midpoint in its iteration directory.
        String fullPath = event.getIteration() == event.getServices().getConfig().controller().getLastIteration()
                ? output.getOutputFilename(PT_FILE_NAME)
                : output.getIterationFilename(event.getIteration(), PT_FILE_NAME);
        try {
            Files.createDirectories(Path.of(fullPath).getParent());
            ptPassengerCountingHandler.writeCSV(fullPath);
            logger.info("PT passenger counts for iteration {} written to: {}", event.getIteration(), fullPath);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write PT passenger counts to " + fullPath, e);
        } finally {
            // Release midpoint data immediately rather than retaining it until the final iteration.
            ptPassengerCountingHandler.reset(event.getIteration());
        }
    }
}
