package org.eqasim.core.components.traffic_light;

import org.eqasim.core.components.traffic_light.delays.IntersectionDelay;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.delays.UnsignalizedIntersectionDelay;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;


public class DelaysUpdaterListener implements IterationEndsListener {
    private final static Logger logger = LogManager.getLogger(DelaysUpdaterListener.class);
    // files names
    private final String FLOW_FILE = "traffic_flow.csv";
    private final String TL_DELAY_FILE = "traffic_light_delay.csv";
    private final String US_DELAY_FILE = "traffic_light_unsignalizedIntersection.csv";
    // writing intervals
    private final int writeFlowInterval;
    private final int writeDelaysInterval;
    // starting iteration
    private final int startIteration;
    // data sets
    private final FlowDataSet flowDataSet;
    private final TrafficCounter counter;
    private final TrafficLightDelay trafficLightDelay;
    private final UnsignalizedIntersectionDelay unsignalizedIntersectionDelay;
    private final IntersectionDelay intersectionDelay;
    // output hierarchy
    private final OutputDirectoryHierarchy outputHierarchy;

    // check which of the delays are activated
    private final boolean activateTrafficLightDelays;
    private final boolean activateUnsignalizedDelays;
    private final boolean moduleActivated;

    public DelaysUpdaterListener(DelaysConfigGroup delaysConfigGroup, FlowDataSet flowDataSet, TrafficCounter trafficCounter,
                                 TrafficLightDelay trafficLightDelay, UnsignalizedIntersectionDelay unsignalizedIntersectionDelay,
                                 OutputDirectoryHierarchy outputHierarchy, IntersectionDelay intersectionDelay) {

        this.writeFlowInterval = delaysConfigGroup.getWriteFlowInterval();
        this.writeDelaysInterval = delaysConfigGroup.getWriteDelayInterval();
        this.startIteration = delaysConfigGroup.getStartingIteration();

        this.moduleActivated = delaysConfigGroup.isActivated();
        this.activateTrafficLightDelays = delaysConfigGroup.isTlActivated();
        this.activateUnsignalizedDelays = delaysConfigGroup.isUnsignalizedActivated();

        this.flowDataSet = flowDataSet;
        this.counter = trafficCounter;
        this.trafficLightDelay = trafficLightDelay;
        this.unsignalizedIntersectionDelay = unsignalizedIntersectionDelay;
        this.intersectionDelay = intersectionDelay;
        this.outputHierarchy = outputHierarchy;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        if (!moduleActivated) {
            return;
        }

        int iteration = event.getIteration();
        if (iteration==0){
            // init the flow map at the first iteration
            flowDataSet.initializeFlowMap();
            // we make sure the traffic light attribute is set to all links
            trafficLightDelay.setTlAttributeToAllLinks();
            // then we initialize the trafficLightDelays map for the links that have a traffic light attribute set to true
            if (activateTrafficLightDelays) {
                trafficLightDelay.initDelays();
            }
            // then do the same with the unsignalized intersections
            if (activateUnsignalizedDelays) {
                unsignalizedIntersectionDelay.initDelays();
            }
        }

        // update the flow dataset at the end of each iteration and write it if required
        flowDataSet.updateFlow(iteration, counter);
        writeFlowDataIfRequired(event);

        // update the intersection delay at the end of each iteration
        intersectionDelay.updateIteration(iteration);

        // reset delays and write delay data if the iteration is greater than or equal to the starting iteration
        if (iteration >= startIteration-1) { //-1 because if we want to start at iteration i, we need to reset at the end of iteration i-1
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

    private void writeDelayData(IterationEndsEvent event, String delayType) {
        if (event.getIteration() >= startIteration-1 && writeDelaysInterval > 0) {
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

    private void writeFlowDataIfRequired(IterationEndsEvent event) {
        if (writeFlowInterval > 0 && (event.getIteration() % writeFlowInterval == 0 || event.isLastIteration())) {
            File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));

            logger.info("Writing FlowDataSet data to " + outputFile.toString() + "...");
            try {
                flowDataSet.exportToCSV(String.valueOf(outputFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("  Done");

        }
    }
}
