package org.eqasim.core.simulation.modes.drt.analysis.run;

import org.eqasim.core.simulation.modes.drt.analysis.passengers.PassengerAnalysisListener;
import org.eqasim.core.simulation.modes.drt.analysis.passengers.PassengerAnalysisWriter;
import org.eqasim.core.simulation.modes.drt.analysis.utils.LinkFinder;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class RunDrtPassengerAnalysis {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "network-path", "output-path", "modes") //
				.build();

		String eventsPath = cmd.getOptionStrict("events-path");
		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");

		String rawModes = cmd.getOptionStrict("modes");
		Set<String> modes = Arrays.asList(rawModes.split(",")).stream().map(String::trim).collect(Collectors.toSet());

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		LinkFinder linkFinder = new LinkFinder(network);
		VehicleRegistry vehicleRegistry = new VehicleRegistry();
		PassengerAnalysisListener listener = new PassengerAnalysisListener(modes, linkFinder, vehicleRegistry);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(vehicleRegistry);
		eventsManager.addHandler(listener);

		eventsManager.initProcessing();
		DrtEventsReaders.createEventsReader(eventsManager).readFile(eventsPath);
		eventsManager.finishProcessing();

		new PassengerAnalysisWriter(listener).writeRides(new File(outputPath));
	}
}
