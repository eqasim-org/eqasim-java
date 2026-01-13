package org.eqasim.core.components.traffic_light;

import org.eqasim.core.components.traffic_light.delays.IntersectionDelay;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.delays.UnsignalizedIntersectionDelay;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.core.controler.listener.IterationStartsListener;

import java.io.File;
import java.io.IOException;


public class DelaysUpdaterListener implements IterationStartsListener {
    private final static Logger logger = LogManager.getLogger(DelaysUpdaterListener.class);
    // files names
    private final String TL_DELAY_FILE = "traffic_light_delay.csv";
    private final String US_DELAY_FILE = "traffic_light_unsignalizedIntersection.csv";
    // writing intervals
    private final int writeDelaysInterval;
    // starting iteration
    private final int startIteration;
    // data sets
    private final TrafficLightDelay trafficLightDelay;
    private final UnsignalizedIntersectionDelay unsignalizedIntersectionDelay;
    private final IntersectionDelay intersectionDelay;
    // output hierarchy
    private final OutputDirectoryHierarchy outputHierarchy;

    // check which of the delays are activated
    private final boolean activateTrafficLightDelays;
    private final boolean activateUnsignalizedDelays;
    private final boolean moduleActivated;

    public DelaysUpdaterListener(DelaysConfigGroup delaysConfigGroup, FlowDataSet flowDataSet, LinkFlowCounter linkFlowCounter,
                                 TrafficLightDelay trafficLightDelay, UnsignalizedIntersectionDelay unsignalizedIntersectionDelay,
                                 OutputDirectoryHierarchy outputHierarchy, IntersectionDelay intersectionDelay) {

        this.writeDelaysInterval = delaysConfigGroup.getWriteDelayInterval();
        this.startIteration = delaysConfigGroup.getStartingIteration();

        this.moduleActivated = delaysConfigGroup.isActivated();
        this.activateTrafficLightDelays = delaysConfigGroup.isTlActivated();
        this.activateUnsignalizedDelays = delaysConfigGroup.isUnsignalizedActivated();

        this.trafficLightDelay = trafficLightDelay;
        this.unsignalizedIntersectionDelay = unsignalizedIntersectionDelay;
        this.intersectionDelay = intersectionDelay;
        this.outputHierarchy = outputHierarchy;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        // if the module is not activated, do nothing
        if (!moduleActivated) {
            return;
        }
        // if none of the delays is activated, do nothing
        if (!activateTrafficLightDelays && !activateUnsignalizedDelays) {
            return;
        }

        int iteration = event.getIteration();
        if (iteration==0){
            if (activateTrafficLightDelays) {
                // we make sure the traffic light attribute is set to all links
                trafficLightDelay.setTlAttributeToAllLinks();
                // then we initialize the trafficLightDelays map for the links that have a traffic light attribute set to true
                trafficLightDelay.initDelays();
            }
            // then do the same with the unsignalized intersections
            if (activateUnsignalizedDelays) {
                unsignalizedIntersectionDelay.initDelays();
            }
        }

        // tell the intersection delay about the iteration
        intersectionDelay.updateIteration(iteration);

        // reset delays and write delay data if the iteration is greater than or equal to the starting iteration
        if (iteration >= startIteration) {
            if (activateTrafficLightDelays) {
                trafficLightDelay.resetDelays();
                writeDelayData(event, "traffic_light");
            }
            if (activateUnsignalizedDelays) {
                unsignalizedIntersectionDelay.resetDelays();
                writeDelayData(event, "unsignalized");
            }
        }

    }

    private void writeDelayData(IterationStartsEvent event, String delayType) {
        if (event.getIteration() >= startIteration && writeDelaysInterval > 0) {
            if (event.getIteration() % writeDelaysInterval == 0 || event.isLastIteration()) {
                String delayFile = switch (delayType) {
                    case "traffic_light" -> TL_DELAY_FILE;
                    case "unsignalized" -> US_DELAY_FILE;
                    default -> throw new IllegalStateException("Unexpected value: " + delayType);
                };
                
                File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), delayFile));

                logger.info("Writing "+delayType+" delays data to " + outputFile.toString() + "...");
                try {
                    if (delayType.equals("traffic_light")){
                        trafficLightDelay.exportToCSV(String.valueOf(outputFile));
                    } else {
                        unsignalizedIntersectionDelay.exportToCSV(String.valueOf(outputFile));
                    }

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                logger.info("  Done");

            }
        }
    }
}
