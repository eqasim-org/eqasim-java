package org.eqasim.core.tools;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
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

		SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();

		featureTypeBuilder.setName("network");
		featureTypeBuilder.setCRS(crs);
		featureTypeBuilder.setDefaultGeometry("geometry");

		featureTypeBuilder.add("link", String.class);
		featureTypeBuilder.add("from", String.class);
		featureTypeBuilder.add("to", String.class);
		featureTypeBuilder.add("osm", String.class);
		featureTypeBuilder.add("lanes", Integer.class);
		featureTypeBuilder.add("capacity", Double.class);
		featureTypeBuilder.add("freespeed", Double.class);
		featureTypeBuilder.add("geometry", LineString.class);

		SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		Collection<SimpleFeature> features = new LinkedList<>();

		GeometryFactory geometryFactory = new GeometryFactory();

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

				featureBuilder.add(link.getId().toString());
				featureBuilder.add(link.getFromNode().getId().toString());
				featureBuilder.add(link.getToNode().getId().toString());
				featureBuilder.add(link.getAttributes().getAttribute("osm:way:highway"));
				featureBuilder.add(link.getNumberOfLanes());
				featureBuilder.add(link.getCapacity());
				featureBuilder.add(link.getFreespeed());

				featureBuilder.add(geometryFactory.createLineString(new Coordinate[] { fromCoordinate, toCoordinate }));
				features.add(featureBuilder.buildFeature(null));
			}
		}

		// Wrap up
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		featureCollection.addAll(features);

		// Write
		File outputPath = new File(cmd.getOptionStrict("output-path"));

		if (outputPath.exists()) {
			outputPath.delete();
		}

		GeoPackage outputPackage = new GeoPackage(outputPath);
		outputPackage.init();

		FeatureEntry featureEntry = new FeatureEntry();
		outputPackage.add(featureEntry, featureCollection);

		outputPackage.close();
	}
}