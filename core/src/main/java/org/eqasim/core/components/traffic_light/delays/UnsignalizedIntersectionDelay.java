package org.eqasim.core.components.traffic_light.delays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparDelay;
import org.matsim.api.core.v01.network.Link;

import java.io.IOException;

public class UnsignalizedIntersectionDelay {
    private final Logger logger = LogManager.getLogger(UnsignalizedIntersectionDelay.class);
    private final ShahparDelay intersectionDelayFormula;

    public UnsignalizedIntersectionDelay(ShahparDelay delayFormula) {
        this.intersectionDelayFormula = delayFormula;
        logger.info("Unsignalized intersection delay initialized with Shahpar formula.");
    }

    public double getDelay(Link link, double time){
        // 1. Check if the intersection node has a degree assigned, and this degree is higher then 2 (otherwise it is a simple connection)
        if (!intersectionDelayFormula.considerLink(link)) {
            return 0.0;
        }
        // 3. return the delay using the formula
        return intersectionDelayFormula.getDelay(link, time);
    }

    public void initDelays() {
        intersectionDelayFormula.initDelays();
    }

    public void resetDelays() {
        intersectionDelayFormula.resetDelays();
    }

    public void exportToCSV(String filename) throws IOException {
        intersectionDelayFormula.exportToCSV(filename);
    }
}
