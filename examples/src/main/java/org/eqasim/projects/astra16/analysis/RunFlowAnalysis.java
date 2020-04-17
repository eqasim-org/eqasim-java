package org.eqasim.projects.astra16.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
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
import org.matsim.core.utils.gis.PolylineFeatureFactory;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class RunFlowAnalysis {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "network-path", "output-path").build();

		EventsManager eventsManager = EventsUtils.createEventsManager();
		Listener listener = new Listener();
		eventsManager.addHandler(listener);
		new MatsimEventsReader(eventsManager).readFile(cmd.getOptionStrict("events-path"));

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		CoordinateReferenceSystem crs = MGC.getCRS("EPSG:2056"); // EPSG Code for Swiss CH1903_LV03 coordinate system
		Collection<SimpleFeature> features = new LinkedList<>();

		PolylineFeatureFactory.Builder featureFactoryBuilder = new PolylineFeatureFactory.Builder() //
				.setCrs(crs) //
				.setName("link") //
				.addAttribute("link_id", String.class);

		for (int i = 0; i < 30; i++) {
			featureFactoryBuilder.addAttribute("flow_" + i, Integer.class);
			featureFactoryBuilder.addAttribute("avflow_" + i, Integer.class);
		}

		PolylineFeatureFactory featureFactory = featureFactoryBuilder.create();

		for (Link link : network.getLinks().values()) {
			List<Integer> allLinkFlow = listener.allFlow.get(link.getId());
			List<Integer> avLinkFlow = listener.avFlow.get(link.getId());

			if (allLinkFlow != null) {
				Coord fromCoord = link.getFromNode().getCoord();
				Coord toCoord = link.getToNode().getCoord();

				Coordinate fromCoordinate = new Coordinate(fromCoord.getX(), fromCoord.getY());
				Coordinate toCoordinate = new Coordinate(toCoord.getX(), toCoord.getY());

				if (link.getAllowedModes().contains("car")) {
					Object[] attributes = new Object[61];
					attributes[0] = link.getId().toString();

					for (int i = 0; i < 30; i++) {
						attributes[i * 2 + 1] = allLinkFlow.get(i);
						attributes[i * 2 + 2] = avLinkFlow.get(i);
					}

					features.add(featureFactory.createPolyline(new Coordinate[] { fromCoordinate, toCoordinate },
							attributes, null));
				}
			}
		}

		ShapeFileWriter.writeGeometries(features, cmd.getOptionStrict("output-path"));

	}

	static private class Listener implements LinkEnterEventHandler {
		final Map<Id<Link>, List<Integer>> allFlow = new HashMap<>();
		final Map<Id<Link>, List<Integer>> avFlow = new HashMap<>();

		@Override
		public void handleEvent(LinkEnterEvent event) {
			Id<Link> linkId = event.getLinkId();
			int hour = (int) Math.floor(event.getTime() / 3600.0);

			if (hour >= 0 && hour < 30) {
				if (!allFlow.containsKey(linkId)) {
					allFlow.put(linkId, new ArrayList<>(Collections.nCopies(30, 0)));
					avFlow.put(linkId, new ArrayList<>(Collections.nCopies(30, 0)));
				}

				if (event.getVehicleId().toString().startsWith("av:")) {
					List<Integer> avLinkFlow = avFlow.get(linkId);
					avLinkFlow.set(hour, avLinkFlow.get(hour) + 1);
				}

				List<Integer> allLinkFlow = allFlow.get(linkId);
				allLinkFlow.set(hour, allLinkFlow.get(hour) + 1);
			}
		}
	}
}
