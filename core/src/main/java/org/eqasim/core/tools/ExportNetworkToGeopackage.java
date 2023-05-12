package org.eqasim.core.tools;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class ExportNetworkToGeopackage {
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
				.addAttribute("lanes", Integer.class) //
				.addAttribute("capacity", Double.class) //
				.addAttribute("freespeed", Double.class) //
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
								link.getAttributes().getAttribute("osm:way:highway"), //
								link.getNumberOfLanes(), //
								link.getCapacity(), //
								link.getFreespeed(), //
						}, null);

				features.add(feature);
			}
		}

		// Wrap up
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		featureCollection.addAll(features);

		// Write
		GeoPackage outputPackage = new GeoPackage(new File(cmd.getOptionStrict("output-path")));
		outputPackage.init();

		FeatureEntry featureEntry = new FeatureEntry();
		outputPackage.add(featureEntry, featureCollection);

		outputPackage.close();
	}
}