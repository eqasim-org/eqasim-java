package org.eqasim.core.simulation.modes.drt.analysis.run;

import org.eqasim.core.simulation.modes.drt.analysis.dvrp_vehicles.VehicleAnalysisListener;
import org.eqasim.core.simulation.modes.drt.analysis.dvrp_vehicles.VehicleAnalysisWriter;
import org.eqasim.core.simulation.modes.drt.analysis.utils.LinkFinder;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.drt.util.DrtEventsReaders;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

import java.io.File;
import java.io.IOException;

public class RunDrtVehicleAnalysis {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "network-path", "movements-output-path", "activities-output-path") //
				.build();

		String eventsPath = cmd.getOptionStrict("events-path");
		String networkPath = cmd.getOptionStrict("network-path");
		String movementsOutputPath = cmd.getOptionStrict("movements-output-path");
		String activitiesOutputPath = cmd.getOptionStrict("activities-output-path");

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		LinkFinder linkFinder = new LinkFinder(network);
		VehicleRegistry vehicleRegistry = new VehicleRegistry();
		VehicleAnalysisListener listener = new VehicleAnalysisListener(linkFinder, vehicleRegistry);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(vehicleRegistry);
		eventsManager.addHandler(listener);

		eventsManager.initProcessing();
		DrtEventsReaders.createEventsReader(eventsManager).readFile(eventsPath);
		eventsManager.finishProcessing();

		new VehicleAnalysisWriter(listener).writeMovements(new File(movementsOutputPath));
		new VehicleAnalysisWriter(listener).writeActivities(new File(activitiesOutputPath));
	}
}
