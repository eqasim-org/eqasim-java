package org.eqasim.core.simulation.vdf;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.simulation.vdf.analysis.FlowWriter;
import org.eqasim.core.simulation.vdf.handlers.VDFTrafficHandler;
import org.eqasim.core.simulation.vdf.travel_time.VDFTravelTime;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.controler.listener.StartupListener;

import com.google.common.io.Files;

public class VDFUpdateListener implements IterationEndsListener, StartupListener, ShutdownListener {
	private final static Logger logger = LogManager.getLogger(VDFUpdateListener.class);

	private final String VDF_FILE = "vdf.bin";
	private final String FLOW_FILE = "vdf_flow.csv";

	private final VDFScope scope;
	private final VDFTrafficHandler handler;
	private final VDFTravelTime travelTime;

	private final int writeInterval;
	private final int writeFlowInterval;
	private final URL inputFile;

	private final int firstIteration;

	private final OutputDirectoryHierarchy outputHierarchy;
	private final Network network;

	public VDFUpdateListener(Network network, VDFScope scope, VDFTrafficHandler handler, VDFTravelTime travelTime,
			OutputDirectoryHierarchy outputHierarchy, int writeInterval, int writeFlowInterval, int firstIteration,
			URL inputFile) {
		this.network = network;
		this.scope = scope;
		this.handler = handler;
		this.travelTime = travelTime;
		this.writeInterval = writeInterval;
		this.writeFlowInterval = writeFlowInterval;
		this.outputHierarchy = outputHierarchy;
		this.inputFile = inputFile;
		this.firstIteration = firstIteration;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		// ignore when restarting simulation as "first iteration" will produce
		// information that is already in the cache, hence the call to aggregate is just
		// to obtain historical flows
		boolean ignoreIteration = event.getIteration() == firstIteration && inputFile != null;

		IdMap<Link, List<Double>> data = handler.aggregate(ignoreIteration);
		scope.verify(data, "Wrong flow format");
		travelTime.update(data);

		if (writeInterval > 0 && (event.getIteration() % writeInterval == 0 || event.isLastIteration())) {
			File outputFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), VDF_FILE));

			logger.info("Writing VDF data to " + outputFile.toString() + "...");
			handler.getWriter().writeFile(outputFile);
			logger.info("  Done");

		}

		if (writeFlowInterval > 0 && (event.getIteration() % writeFlowInterval == 0 || event.isLastIteration())) {
			File flowFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));

			logger.info("Writing flow information to " + flowFile.toString() + "...");
			new FlowWriter(data, network, scope).write(flowFile);
			logger.info("  Done");
		}
	}

	@Override
	public void notifyStartup(StartupEvent event) {
		if (inputFile != null) {
			logger.info("Reading VDF data from " + inputFile.toString() + "...");

			handler.getReader().readFile(inputFile);

			// ignore "current iteration" because it does not exist at startup, we just
			// aggregate to have consistent travel times
			IdMap<Link, List<Double>> data = handler.aggregate(true);
			scope.verify(data, "Wrong flow format");
			travelTime.update(data, true);

			logger.info("  Done");
		}
	}

	@Override
	public void notifyShutdown(ShutdownEvent event) {
		try {
			File fromFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), VDF_FILE));
			File toFile = new File(outputHierarchy.getOutputFilename(VDF_FILE));

			if (fromFile.exists()) {
				Files.copy(fromFile, toFile);
			}

			File fromFlowFile = new File(outputHierarchy.getIterationFilename(event.getIteration(), FLOW_FILE));
			File toFlowFile = new File(outputHierarchy.getOutputFilename(FLOW_FILE));

			if (fromFlowFile.exists()) {
				Files.copy(fromFlowFile, toFlowFile);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
