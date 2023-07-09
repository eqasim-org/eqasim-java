package org.eqasim.cairo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.io.UncheckedIOException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunCountAnalysis {
	static public void main(String[] args) throws IOException, UncheckedIOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "events-path", "output-path", "crs") //
				.build();

		// Reading part
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		EventsManager eventsManager = EventsUtils.createEventsManager();

		IdMap<Link, Integer> counts = new IdMap<>(Link.class);
		eventsManager.addHandler(new CountHandler(counts));

		new MatsimEventsReader(eventsManager).readFile(cmd.getOptionStrict("events-path"));

		// Writing part
		CoordinateReferenceSystem crs = MGC.getCRS(cmd.getOptionStrict("crs"));

		SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();

		featureTypeBuilder.setName("count");
		featureTypeBuilder.setCRS(crs);
		featureTypeBuilder.setDefaultGeometry("geometry");

		featureTypeBuilder.add("link_id", String.class);
		featureTypeBuilder.add("from_id", String.class);
		featureTypeBuilder.add("to_id", String.class);
		featureTypeBuilder.add("osm", String.class);
		featureTypeBuilder.add("lanes", Integer.class);
		featureTypeBuilder.add("capacity", Double.class);
		featureTypeBuilder.add("freespeed", Double.class);
		featureTypeBuilder.add("count", Double.class);
		featureTypeBuilder.add("geometry", LineString.class);

		SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();

		SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
		Collection<SimpleFeature> features = new LinkedList<>();

		GeometryFactory geometryFactory = new GeometryFactory();

		for (Link link : network.getLinks().values()) {
			boolean isSelected = link.getAllowedModes().contains("car");

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
				featureBuilder.add(counts.getOrDefault(link.getId(), 0));

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

	static private class CountHandler implements LinkEnterEventHandler {
		private final IdMap<Link, Integer> counts;

		CountHandler(IdMap<Link, Integer> counts) {
			this.counts = counts;
		}

		@Override
		public void handleEvent(LinkEnterEvent event) {
			counts.compute(event.getLinkId(), (id, value) -> value == null ? 1 : value + 1);
		}
	}
}
