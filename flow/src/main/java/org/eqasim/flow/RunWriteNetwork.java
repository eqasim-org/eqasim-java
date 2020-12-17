package org.eqasim.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunWriteNetwork {
	static public void main(String[] args) throws ConfigurationException, InterruptedException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "output-path", "crs") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		Network roadNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(network).filter(roadNetwork, Collections.singleton("car"));

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));
		Collection<SimpleFeature> features = new ArrayList<>(roadNetwork.getLinks().size());

		PolylineFeatureFactory featureFactory = new PolylineFeatureFactory.Builder() //
				.setCrs(crs) //
				.setName("links") //
				.addAttribute("linkId", String.class) //
				.addAttribute("osmType", String.class) //
				//
				.create();

		for (Link link : roadNetwork.getLinks().values()) {
			String osmType = (String) link.getAttributes().getAttribute("osm:way:highway");

			Coordinate fromCoord = new Coordinate(link.getFromNode().getCoord().getX(),
					link.getFromNode().getCoord().getY());
			Coordinate toCoord = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());

			SimpleFeature feature = featureFactory.createPolyline(new Coordinate[] { fromCoord, toCoord },
					new Object[] { //
							link.getId().toString(), //
							osmType, //
					}, null);
			features.add(feature);
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}
