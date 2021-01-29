package org.eqasim.core.tools;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportNetworkToShapefile {
	public static void main(String[] args) throws Exception {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "output-path", "crs") //
				.allowOptions("modes") //
				.build();

		String networkPath = cmd.getOptionStrict("network-path");

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));
		Collection<String> modes = new HashSet<>();

		Arrays.asList(cmd.getOption("modes").orElse("car").split(",")).forEach(mode -> {
			modes.add(mode.trim());
		});

		Collection<SimpleFeature> features = new LinkedList<>();

		PolylineFeatureFactory linkFactory = new PolylineFeatureFactory.Builder() //
				.setCrs(crs).setName("link") //
				.addAttribute("link", String.class) //
				.addAttribute("from", String.class) //
				.addAttribute("to", String.class) //
				.addAttribute("osm", String.class) //
				.create();

		for (Link link : network.getLinks().values()) {
			boolean isSelected = false;

			for (String mode : link.getAllowedModes()) {
				if (modes.contains(mode)) {
					isSelected = true;
				}
			}

			if (isSelected) {
				Coordinate fromCoordinate = new Coordinate(link.getFromNode().getCoord().getX(),
						link.getFromNode().getCoord().getY());
				Coordinate toCoordinate = new Coordinate(link.getToNode().getCoord().getX(),
						link.getToNode().getCoord().getY());

				SimpleFeature feature = linkFactory.createPolyline( //
						new Coordinate[] { fromCoordinate, toCoordinate }, //
						new Object[] { //
								link.getId().toString(), //
								link.getFromNode().getId().toString(), //
								link.getToNode().getId().toString(), //
								link.getAttributes().getAttribute("osm:way:highway") //
						}, null);

				features.add(feature);
			}
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));
	}
}