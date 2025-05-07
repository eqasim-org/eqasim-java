package org.eqasim.switzerland.ch.utils;

import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;


public class PTPassengerCountingActivator implements IterationStartsListener {
    private final PTPassengerCountingHandler handler;

    public PTPassengerCountingActivator(PTPassengerCountingHandler handler) {
        this.handler = handler;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() == event.getServices().getConfig().controller().getLastIteration()) {
            handler.enable();
        } else {
            handler.disable(); // optional, in case reset isn't called
        }
    }
}