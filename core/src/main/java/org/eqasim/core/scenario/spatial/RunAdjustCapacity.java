package org.eqasim.core.scenario.spatial;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eqasim.core.scenario.SpatialUtils;
import org.locationtech.jts.geom.Polygon;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.network.io.NetworkWriter;

public class RunAdjustCapacity {
	static public void main(String[] args) throws ConfigurationException, InterruptedException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path", "shape-path", "shape-attribute", "shape-value", "factor") //
				.build();

		String shapeAttribute = cmd.getOptionStrict("shape-attribute");
		String shapeValue = cmd.getOptionStrict("shape-value");
		URL shapeUrl = new File(cmd.getOptionStrict("shape-path")).toURI().toURL();

		Polygon polygon = SpatialUtils.loadPolygon(shapeUrl, shapeAttribute, shapeValue);
		double factor = Double.parseDouble(cmd.getOptionStrict("factor"));

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("input-path"));

		new AdjustCapacity(polygon, factor).run(network);
		new NetworkWriter(network).write(cmd.getOptionStrict("output-path"));
	}
}
