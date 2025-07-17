package org.eqasim.switzerland.ch.utils.pricing.stopvisiteventhandler;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;

public class StopVisitAnalyzer  implements IterationStartsListener, IterationEndsListener{

    private final StopVisitLogger stopVisitLogger;

    @Inject
    public StopVisitAnalyzer(StopVisitLogger stopVisitLogger){
        this.stopVisitLogger = stopVisitLogger;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() == event.getServices().getConfig().controller().getFirstIteration()) {
            event.getServices().getEvents().addHandler(stopVisitLogger);
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
    }
    
}
