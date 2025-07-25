package org.eqasim.core.components.traffic_light;

import org.eqasim.core.components.traffic_light.TrafficLightConfigGroup;
import org.eqasim.core.components.traffic_light.delays.TrafficLightDelay;
import org.eqasim.core.components.traffic_light.flow.FlowDataSet;
import org.eqasim.core.components.traffic_light.flow.TrafficCounter;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class TrafficLightListener implements IterationEndsListener {
    private final static Logger logger = LogManager.getLogger(TrafficLightListener.class);
    // files names
    private final String FLOW_FILE = "traffic_flow.csv";
    private final String DELAY_FILE = "traffic_light_delay.csv";
    // writing intervals
    private final int writeFlowInterval;
    private final int writeDelaysInterval;
    // starting iteration
    private final int startIteration;
    // data sets
    private final FlowDataSet flowDataSet;
    private final TrafficCounter counter;
    private final TrafficLightDelay trafficLightDelay;
    // output hierarchy
    private final OutputDirectoryHierarchy outputHierarchy;



    public TrafficLightListener(TrafficLightConfigGroup trafficLightConfigGroup, FlowDataSet flowDataSet, TrafficCounter trafficCounter,
                                TrafficLightDelay trafficLightDelay, OutputDirectoryHierarchy outputHierarchy) {
        this.writeFlowInterval = trafficLightConfigGroup.getWriteFlowInterval();
        this.writeDelaysInterval = trafficLightConfigGroup.getWriteDelayInterval();
        this.startIteration = trafficLightConfigGroup.getTlStartingIteration();

        this.flowDataSet = flowDataSet;
        this.counter = trafficCounter;
        this.trafficLightDelay = trafficLightDelay;
        this.outputHierarchy = outputHierarchy;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();
        // update the flow dataset at the end of each iteration
        flowDataSet.updateFlow(iteration, counter);
        // write flow data
        writeFlowData(event);

        // reset delays and write delay data if the iteration is greater than or equal to the starting iteration
        if (iteration >= startIteration-1) { //-1 because if we want to start at iteration i, we need to reset at the end of iteration i-1
            trafficLightDelay.resetDelays(iteration, flowDataSet);
            writeDelayData(event);
        }
    }

    private void writeDelayData(IterationEndsEvent event) {
        if (event.getIteration() >= startIteration && writeDelaysInterval > 0) {
            if (event.getIteration() % writeDelaysInterval == 0 || event.isLastIteration()) {
                File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), DELAY_FILE));

                logger.info("Writing traffic light delays data to " + outputFile.toString() + "...");
                try {
                    trafficLightDelay.exportToCSV(String.valueOf(outputFile));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                logger.info("  Done");

            }
        }
    }

    private void writeFlowData(IterationEndsEvent event) {
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
