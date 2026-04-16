package org.eqasim.switzerland.ch_cmdp.utils.pt;

import java.io.IOException;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;


public class PTPassengerAnalyser implements IterationStartsListener, IterationEndsListener {
	private static final String PT_FILE_NAME = "pt_passenger_counts.csv.gz";

    private final PTPassengerCountingHandler ptPassengerCountingHandler;
    private boolean analysisActive = false;
    
    @Inject
    public PTPassengerAnalyser(PTPassengerCountingHandler ptPassengerCountingHandler) {
        this.ptPassengerCountingHandler = ptPassengerCountingHandler;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() == event.getServices().getConfig().controller().getLastIteration()) {
        	event.getServices().getEvents().addHandler(ptPassengerCountingHandler);
        	analysisActive = true;
        }
    }
    
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
    	if (analysisActive) {
    		analysisActive = false;
            event.getServices().getEvents().removeHandler(ptPassengerCountingHandler);
    		@SuppressWarnings("deprecation")
            String fullPath = event.getServices().getControlerIO().getOutputFilename(PT_FILE_NAME);
    		try {
				ptPassengerCountingHandler.writeCSV(fullPath);
	            System.out.println("✔ PT passenger counts written to: " + fullPath);

			} catch (IOException e) {
				e.printStackTrace();
			}
           
    	}
    }
    
    
}