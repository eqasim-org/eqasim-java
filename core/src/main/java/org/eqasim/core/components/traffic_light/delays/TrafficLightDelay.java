package org.eqasim.core.components.traffic_light.delays;

import org.eqasim.core.components.traffic_light.DelaysConfigGroup;
import org.eqasim.core.components.traffic_light.delays.webster.WebsterDelay;
import org.eqasim.core.components.flow.TimeBinManager;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class TrafficLightDelay {
    private final Logger logger = LogManager.getLogger(TrafficLightDelay.class);

    static public String TL_ATTRIBUTE = "traffic_light";
    private final TimeBinManager timeBinManager;
    private final Network network;
    private final WebsterDelay websterDelay;

    // flags
    public static final double NO_TL = -1; // No traffic light
    public static final double OUT_OF_BOUNDS = -2; // Time out of bounds
    public static final double INCORRECT_DELAY = -3; // After the traffic light module ends

    public TrafficLightDelay(Network network, TimeBinManager timeBinManager,
                             DelaysConfigGroup delaysConfigGroup,
                             WebsterDelay websterDelay) {
        this.timeBinManager = timeBinManager;
        this.network = network;
        this.websterDelay = websterDelay;
        logger.info("Traffic light delays initialized with Webster formula");
    }

    public double getDelay(Link link, double time) {
        if (!hasTrafficLight(link)) {
            return NO_TL; // The link does not have a traffic light (or no trafficLightDelays set)
        }
        if (time < timeBinManager.getTlStartTime() || time > timeBinManager.getTlEndTime()){
            return OUT_OF_BOUNDS; // Time is out of bounds of the time bins (normal crossing penalty will be applied)
        }
        Double delayValue = websterDelay.getDelay(link, time);
        return delayValue != null ? delayValue : INCORRECT_DELAY; // Default to 0 if no delay is set
    }

    public void setTlAttributeToAllLinks() {
        // Set the traffic light attribute to all links in the network
        logger.info("Setting Tl attribute to all links");
        for (Link link : network.getLinks().values()) {
            Object hasTl = link.getAttributes().getAttribute(TL_ATTRIBUTE);
            if (hasTl==null) {
                link.getAttributes().putAttribute(TL_ATTRIBUTE, false); // Default to false
            } else  {
                // if it is just a road connected, this must be ignored
                boolean validIntersectionNodeDegree = link.getToNode().getInLinks().size()>1 || link.getToNode().getOutLinks().size()>1;
                boolean hasTlAsBool = validIntersectionNodeDegree && Boolean.parseBoolean(hasTl.toString());
                link.getAttributes().putAttribute(TL_ATTRIBUTE, hasTlAsBool);
            }
        }
    }

    public void initDelays() {
        websterDelay.initDelays();
    }

    public boolean hasTrafficLight(Link link) {
        // Check if the link has a traffic light attribute set to true
        Object hasTl = link.getAttributes().getAttribute(TL_ATTRIBUTE);
        if (hasTl == null) {
            return false; // No attribute means no traffic light
        }
        return Boolean.parseBoolean(hasTl.toString()); // Safer conversion
    }

    public void resetDelays() {
        websterDelay.resetDelays();
    }

    public void exportToCSV(String filename) throws IOException {
        websterDelay.exportToCSV(filename);
    }

}
