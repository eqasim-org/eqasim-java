package org.eqasim.vdf.handlers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eqasim.vdf.VDFScope;
import org.eqasim.vdf.io.VDFReaderInterface;
import org.eqasim.vdf.io.VDFWriterInterface;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.io.IOUtils;

import com.google.common.base.Verify;

public class VDFInterpolationHandler implements VDFTrafficHandler, LinkEnterEventHandler {
	private final VDFScope scope;

	private final double updateFactor;

	private final IdMap<Link, List<Double>> interpolatedCounts = new IdMap<>(Link.class);
	private final IdMap<Link, List<Double>> currentCounts = new IdMap<>(Link.class);

	public VDFInterpolationHandler(Network network, VDFScope scope, double updateFactor) {
		this.scope = scope;
		this.updateFactor = updateFactor;

		for (Id<Link> linkId : network.getLinks().keySet()) {
			interpolatedCounts.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));
			currentCounts.put(linkId, new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0)));
		}
	}

	@Override
	public synchronized void handleEvent(LinkEnterEvent event) {
		processEnterLink(event.getTime(), event.getLinkId());
	}

	public void processEnterLink(double time, Id<Link> linkId) {
		int i = scope.getIntervalIndex(time);
		double currentValue = currentCounts.get(linkId).get(i);
		currentCounts.get(linkId).set(i, currentValue + 1);
	}

	@Override
	public IdMap<Link, List<Double>> aggregate() {
		interpolatedCounts.forEach((id, interpolated) -> {
			List<Double> current = currentCounts.get(id);

			for (int i = 0; i < interpolated.size(); i++) {
				interpolated.set(i, (1.0 - updateFactor) * interpolated.get(i) + updateFactor * current.get(i));
			}
		});

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

					List<Double> values = new ArrayList<>(Collections.nCopies(scope.getIntervals(), 0.0));
					for (int i = 0; i < values.size(); i++) {
						values.set(i, inputStream.readDouble());
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
				DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(outputFile.toString()));

				outputStream.writeDouble(scope.getStartTime());
				outputStream.writeDouble(scope.getEndTime());
				outputStream.writeDouble(scope.getIntervalTime());
				outputStream.writeDouble(scope.getIntervals());

				for (var entry : interpolatedCounts.entrySet()) {
					outputStream.writeUTF(entry.getKey().toString());

					List<Double> values = entry.getValue();
					for (int i = 0; i < values.size(); i++) {
						outputStream.writeDouble(values.get(i));
					}
				}

				outputStream.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
