package org.eqasim.core.components.flow;

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


public class FlowUpdaterListner implements IterationEndsListener {
    private final static Logger logger = LogManager.getLogger(FlowUpdaterListner.class);

    private final String FLOW_FILE = "traffic_flow.csv";
    private final int writeFlowInterval;
    private final OutputDirectoryHierarchy outputHierarchy;

    private final FlowDataSet flowDataSetData;
    private final TrafficCounter counter;

    public FlowUpdaterListner(FlowConfigGroup flowConfigGroup, FlowDataSet flowDataSet, TrafficCounter trafficCounter,
                              OutputDirectoryHierarchy outputHierarchy) {
        this.writeFlowInterval = flowConfigGroup.getWriteFlowInterval();
        this.flowDataSetData = flowDataSet;
        this.counter = trafficCounter;
        this.outputHierarchy = outputHierarchy;
    }


    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        int iteration = event.getIteration();

        IdMap<Link, List<Double>> iterationCounts = counter.getCounts();
        flowDataSetData.updateFlow(iteration, iterationCounts);

        if (writeFlowInterval > 0 && (event.getIteration() % writeFlowInterval == 0 || event.isLastIteration())) {
            File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));

            logger.info("Writing FlowDataSet data to " + outputFile.toString() + "...");
            try {
                flowDataSetData.exportToCSV(String.valueOf(outputFile));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            logger.info("  Done");

        }
    }
}
