package org.eqasim.core.simulation.vdf.handlers;


import java.io.File;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.flow.FlowUtils;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.io.VDFReaderInterface;
import org.eqasim.core.simulation.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;

public class LowMemoryHandler implements VDFTrafficHandler, LinkEnterEventHandler {

    private final FlowDataSet flowDataSet;
    private final VDFScope scope;
    private final static Logger logger = LogManager.getLogger(LowMemoryHandler.class);

    public LowMemoryHandler(FlowDataSet flowDataSet, VDFScope scope) {
        this.flowDataSet = flowDataSet;
        this.scope = scope;
    }

    @Override
    public synchronized void handleEvent(LinkEnterEvent event) {
        // do nothing, this is done in the linkFlowCounter
    }

    @Override
    public void processEnterLink(double time, Id<Link> linkId, double pcu) {
       // do nothing, this is done in the linkFlowCounter
    }

    @Override
    public IdMap<Link, double[]> aggregate(boolean ignoreIteration) {
        return flowDataSet.getFlowBinMapInDifferentBins(scope.getStartTime(), scope.getEndTime(), scope.getIntervalTime());
    }

    @Override
    public VDFReaderInterface getReader() {
        return new Reader();
    }

    @Override
    public VDFWriterInterface getWriter() {
        return new Writer();
    }

    public class Reader implements VDFReaderInterface {
        @Override
        public void readFile(URL inputFile) {

        }
    }

    public class Writer implements VDFWriterInterface {
        @Override
        public void writeFile(File outputFile) {

        }
    }
}
