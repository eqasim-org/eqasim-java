package org.eqasim.examples.zurich_parking.analysis.parking;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.parking.parkingsearch.events.ParkingEvent;
import org.matsim.contrib.parking.parkingsearch.events.ParkingEventMapper;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventMapper;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.IOException;
import java.util.Collection;

public class ParkingSearchMetricsReaderFromEvents {
	final private ParkingSearchMetricsListener listener;

	static public void main(String[] args) throws CommandLine.ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "network-path", "output-path") //
				.build();

		// read network
		String networkPath = cmd.getOptionStrict("network-path");
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		// read events
		String eventsPath = cmd.getOptionStrict("events-path");
		ParkingSearchMetricsListener lister = new ParkingSearchMetricsListener(network);
		ParkingSearchMetricsReaderFromEvents reader = new ParkingSearchMetricsReaderFromEvents(lister);
		Collection<ParkingSearchItem> items =  reader.read(eventsPath);

		// write out stats
		String outputPath = cmd.getOptionStrict("output-path");
		ParkingSearchMetricsWriter writer = new ParkingSearchMetricsWriter(items);
		writer.write(outputPath);
	}

	public ParkingSearchMetricsReaderFromEvents(ParkingSearchMetricsListener tripListener) {
		this.listener = tripListener;
	}

	public Collection<ParkingSearchItem> read(String eventsPath) {
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(listener);
		MatsimEventsReader eventsReader = new MatsimEventsReader(eventsManager);
		eventsReader.addCustomEventMapper(StartParkingSearchEvent.EVENT_TYPE, new StartParkingSearchEventMapper());
		eventsReader.addCustomEventMapper(ParkingEvent.EVENT_TYPE, new ParkingEventMapper());
		eventsReader.readFile(eventsPath);
		return listener.getTrips();
	}
}