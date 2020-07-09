package org.eqasim.ile_de_france.validation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

public class RunVehicleCounting {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "events-path", "links-path", "output-path") //
				.build();

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(cmd.getOptionStrict("network-path"));

		Collection<SimpleFeature> features = new ShapeFileReader()
				.readFileAndInitialize(cmd.getOptionStrict("links-path"));

		Collection<Id<Link>> linkIds = new HashSet<>();

		for (SimpleFeature feature : features) {
			linkIds.add(Id.createLinkId((String) feature.getAttribute("link_id")));
		}

		Map<Id<Link>, List<Integer>> counts = new HashMap<>();

		for (Id<Link> linkId : linkIds) {
			counts.put(linkId, new ArrayList<>(Collections.nCopies(24, 0)));
		}

		EventsManager eventsManager = EventsUtils.createEventsManager();

		Listener listener = new Listener(counts);
		eventsManager.addHandler(listener);

		new MatsimEventsReader(eventsManager).readFile(cmd.getOptionStrict("events-path"));

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(new File(cmd.getOptionStrict("output-path")))));

		writer.write(String.join(";", new String[] { "link_id", "hour", "count", "lanes" }) + "\n");

		for (Map.Entry<Id<Link>, List<Integer>> item : counts.entrySet()) {
			for (int hour = 0; hour < 24; hour++) {
				writer.write(String.join(";", new String[] { //
						item.getKey().toString(), //
						String.valueOf(hour), //
						String.valueOf(item.getValue().get(hour)), //
						String.valueOf(network.getLinks().get(item.getKey()).getNumberOfLanes()), //
				}) + "\n");
			}
		}

		writer.close();
	}

	static private class Listener implements LinkLeaveEventHandler {
		private final Map<Id<Link>, List<Integer>> counts;

		Listener(Map<Id<Link>, List<Integer>> counts) {
			this.counts = counts;
		}

		@Override
		public void handleEvent(LinkLeaveEvent event) {
			List<Integer> linkCounts = counts.get(event.getLinkId());

			if (linkCounts != null) {
				int hour = (int) Math.floor(event.getTime() / 3600.0);

				if (hour >= 24) {
					hour -= 24;
				}

				linkCounts.set(hour, linkCounts.get(hour) + 1);
			}
		}
	}
}
