package org.eqasim.vdf.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.eqasim.vdf.VDFScope;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class FlowWriter {
	private final IdMap<Link, List<Double>> flows;
	private final Network network;
	private final VDFScope scope;

	public FlowWriter(IdMap<Link, List<Double>> flows, Network network, VDFScope scope) {
		this.flows = flows;
		this.network = network;
		this.scope = scope;
	}

	public void write(File path) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

			writer.write(String.join(";", new String[] { "link_id", "interval", "start_time", "flow", "lanes", "osm" })
					+ "\n");

			for (Map.Entry<Id<Link>, List<Double>> item : flows.entrySet()) {
				for (int interval = 0; interval < scope.getIntervals(); interval++) {
					writer.write(String.join(";", new String[] { //
							item.getKey().toString(), //
							String.valueOf(interval), //
							String.valueOf(scope.getStartTime() + interval * scope.getIntervalTime()), //
							String.valueOf(item.getValue().get(interval)), //
							String.valueOf(network.getLinks().get(item.getKey()).getNumberOfLanes()), //
							String.valueOf(network.getLinks().get(item.getKey()).getAttributes()
									.getAttribute("osm:way:highway")) //
					}) + "\n");
				}
			}

			writer.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
