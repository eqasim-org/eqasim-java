package org.eqasim.core.simulation.vdf.handlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.components.flow.FlowDataSet;
import org.eqasim.core.components.flow.LinkFlowCounter;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.io.VDFReaderInterface;
import org.eqasim.core.simulation.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Verify;

public class LowMemoryHandler implements VDFTrafficHandler, LinkEnterEventHandler {
    private final VDFScope scope;

    private final Network network;
    private final LinkFlowCounter linkFlowCounter;
    private final FlowDataSet flowDataSet;

    private final static Logger logger = LogManager.getLogger(VDFHorizonHandler.class);

    public LowMemoryHandler(Network network, VDFScope scope, LinkFlowCounter linkFlowCounter, FlowDataSet flowDataSet) {
        this.scope = scope;
        this.network = network;
        this.linkFlowCounter = linkFlowCounter;
        this.flowDataSet = flowDataSet;
    }

    @Override
    public synchronized void handleEvent(LinkEnterEvent event) {
        processEnterLink(event.getTime(), event.getLinkId());
    }

    @Override
    public void processEnterLink(double time, Id<Link> linkId) {
        // do nothing
    }

    @Override
    public IdMap<Link, double[]> aggregate(boolean ignoreIteration) {
        return flowDataSet.getFlowMap();
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
