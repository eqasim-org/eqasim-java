package org.eqasim.core.components.traffic_light.delays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.traffic_light.delays.shahpar.ShahparDelay;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import java.util.ArrayList;
import java.util.List;

public class UnsignalizedIntersectionDelay {
    private final Logger logger = LogManager.getLogger(UnsignalizedIntersectionDelay.class);
    private final FlowDataSet flow;
    private final Network network;
    private final ShahparDelay formula;
    //private final List<Double> delays = new ArrayList<>();

    public UnsignalizedIntersectionDelay(FlowDataSet flow, Network network, ShahparDelay formula) {
        this.flow = flow;
        this.network = network;
        this.formula = formula;
        logger.info("Unsignalized intersection delay initialized with Shahpar formula.");
    }

    public double getDelay(Link link, double time){
        return formula.getDelay(link, time);
    }


}
