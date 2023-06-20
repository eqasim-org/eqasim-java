package org.eqasim.vdf.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class FlowWriter {
	private final IdMap<Link, List<Double>> flows;
	private final Network network;

	public FlowWriter(IdMap<Link, List<Double>> flows, Network network) {
		this.flows = flows;
		this.network = network;
	}

	public void write(File path) {
		try {
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path)));

			writer.write(String.join(";", new String[] { "link_id", "hour", "flow", "lanes", "osm" }) + "\n");

			for (Map.Entry<Id<Link>, List<Double>> item : flows.entrySet()) {
				for (int hour = 0; hour < 24; hour++) {
					writer.write(String.join(";", new String[] { //
							item.getKey().toString(), //
							String.valueOf(hour), //
							String.valueOf(item.getValue().get(hour)), //
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
