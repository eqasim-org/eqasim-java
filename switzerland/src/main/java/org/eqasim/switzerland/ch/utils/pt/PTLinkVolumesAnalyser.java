package org.eqasim.switzerland.ch.utils.pt;

import java.io.IOException;

import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import com.google.inject.Inject;


public class PTLinkVolumesAnalyser implements IterationStartsListener, IterationEndsListener {
	private static final String PT_FILE_NAME = "pt_link_volumes.csv.gz";

    private final PTLinkVolumesHandler ptLinkVolumesHandler;
    private boolean analysisActive = false;
    
    @Inject
    public PTLinkVolumesAnalyser(PTLinkVolumesHandler ptLinkVolumesHandler) {
        this.ptLinkVolumesHandler = ptLinkVolumesHandler;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        if (event.getIteration() == event.getServices().getConfig().controller().getLastIteration()) {
        	event.getServices().getEvents().addHandler(ptLinkVolumesHandler);
        	analysisActive = true;
        }
    }
    
    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
    	if (analysisActive) {
    		analysisActive = false;
            event.getServices().getEvents().removeHandler(ptLinkVolumesHandler);
    		@SuppressWarnings("deprecation")
            String fullPath = event.getServices().getControlerIO().getOutputFilename(PT_FILE_NAME);
    		try {
				ptLinkVolumesHandler.writeCSV(fullPath);
	            System.out.println("✔ PT link volumes written to: " + fullPath);

			} catch (IOException e) {
				e.printStackTrace();
			}
           
    	}
    }
    
    
}