package org.eqasim.core.travel_times.handlers;

import org.eqasim.core.travel_times.PredictedTravelTimeWriter;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

public class PredictedTripTravelTimeHandler implements IterationStartsListener, IterationEndsListener {
    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        String outputPath = event.getServices().getControlerIO().getIterationFilename(event.getIteration(), "predicted_travel_times.csv");
        PredictedTravelTimeWriter.initialize(outputPath);
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        PredictedTravelTimeWriter.close();
    }
}
