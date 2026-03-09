package org.eqasim.core.simulation.vdf.handlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eqasim.core.components.flow.FlowUtils;
import org.eqasim.core.simulation.vdf.VDFScope;
import org.eqasim.core.simulation.vdf.io.VDFReaderInterface;
import org.eqasim.core.simulation.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Verify;

public class VDFInterpolationHandler implements VDFTrafficHandler, LinkEnterEventHandler {
	private final VDFScope scope;

	private final double updateFactor;
	private final int numIntervals;
	private final Scenario scenario;
	private final IdMap<Link, double[]> interpolatedCounts = new IdMap<>(Link.class);
	private final IdMap<Link, List<Double>> currentCounts = new IdMap<>(Link.class);

	public VDFInterpolationHandler(Network network, VDFScope scope, double updateFactor, Scenario scenario) {
		this.scope = scope;
		this.updateFactor = updateFactor;
		this.numIntervals = scope.getIntervals();
		this.scenario = scenario;

		for (Id<Link> linkId : network.getLinks().keySet()) {
			interpolatedCounts.put(linkId, new double[numIntervals]);
			currentCounts.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));
		}
	}

	@Override
	public synchronized void handleEvent(LinkEnterEvent event) {
		double pcu = FlowUtils.getVehiclePcu(scenario, event);
		processEnterLink(event.getTime(), event.getLinkId(), pcu);
	}

	public void processEnterLink(double time, Id<Link> linkId, double pcu) {
		if (pcu>1e-6) {
			int i = scope.getIntervalIndex(time);
			double currentValue = currentCounts.get(linkId).get(i);
			currentCounts.get(linkId).set(i, currentValue + 1);
		}
	}

	@Override
	public IdMap<Link, double[]> aggregate(boolean ignoreIteration) {
		for (var item : interpolatedCounts.entrySet()) {
			List<Double> current = currentCounts.get(item.getKey());
			double[] interpolated = item.getValue();

			for (int i = 0; i < numIntervals; i++) {
				if (!ignoreIteration) {
					interpolated[i] =  (1.0 - updateFactor) * interpolated[i] + updateFactor * current.get(i);
				}

				current.set(i, 0.0);
			}
		}

		return interpolatedCounts;
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
			try {
				DataInputStream inputStream = new DataInputStream(IOUtils.getInputStream(inputFile));

				Verify.verify(inputStream.readDouble() == scope.getStartTime());
				Verify.verify(inputStream.readDouble() == scope.getEndTime());
				Verify.verify(inputStream.readDouble() == scope.getIntervalTime());
				Verify.verify(inputStream.readInt() == scope.getIntervals());

				while (inputStream.available() > 0) {
					Id<Link> linkId = Id.createLinkId(inputStream.readUTF());

					double[]  values = new double[numIntervals];
					for (int i = 0; i < numIntervals; i++) {
						values[i] = inputStream.readDouble();
					}

					interpolatedCounts.put(linkId, values);
				}

				inputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public class Writer implements VDFWriterInterface {
		@Override
		public void writeFile(File outputFile) {
			try {
				DataOutputStream outputStream = new DataOutputStream(IOUtils.getOutputStream(outputFile.toURI().toURL(), false));

				outputStream.writeDouble(scope.getStartTime());
				outputStream.writeDouble(scope.getEndTime());
				outputStream.writeDouble(scope.getIntervalTime());
				outputStream.writeInt(scope.getIntervals());

				for (var entry : interpolatedCounts.entrySet()) {
					outputStream.writeUTF(entry.getKey().toString());

					double[] values = entry.getValue();
					for (int i = 0; i < numIntervals; i++) {
						outputStream.writeDouble(values[i]);
					}
				}

				outputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
