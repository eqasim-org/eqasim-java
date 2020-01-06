package org.eqasim.automated_vehicles.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.eqasim.automated_vehicles.components.AvPersonAnalysisFilter;
import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.TripItem;
import org.eqasim.core.analysis.TripListener;
import org.eqasim.core.analysis.TripReaderFromEvents;
import org.eqasim.core.analysis.TripReaderFromPopulation;
import org.eqasim.core.analysis.TripWriter;
import org.eqasim.core.components.EqasimMainModeIdentifier;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.pt.PtConstants;

public class RunTripAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("network-path", "output-path") //
				.allowOptions("population-path", "events-path") //
				.allowOptions("stage-activity-types", "network-modes") //
				.allowOptions("input-distance-units", "output-distance-units") //
				.build();

		if (!(cmd.hasOption("population-path") ^ cmd.hasOption("events-path"))) {
			throw new IllegalStateException("Either population-path or events-path must be provided.");
		}

		String networkPath = cmd.getOptionStrict("network-path");
		String outputPath = cmd.getOptionStrict("output-path");

		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(networkPath);

		String plainStageActivityTypes = cmd.getOption("stage-activity-types")
				.orElse(PtConstants.TRANSIT_ACTIVITY_TYPE);
		StageActivityTypesImpl stageActivityTypes = new StageActivityTypesImpl(Arrays
				.asList(plainStageActivityTypes.split(",")).stream().map(s -> s.trim()).collect(Collectors.toSet()));

		MainModeIdentifier mainModeIdentifier = new EqasimMainModeIdentifier();

		Collection<String> networkModes = Arrays.asList(cmd.getOption("network-modes").orElse("car,av").split(","))
				.stream().map(s -> s.trim()).collect(Collectors.toSet());

		Collection<TripItem> trips = null;

		if (cmd.hasOption("events-path")) {
			String eventsPath = cmd.getOptionStrict("events-path");
			TripListener tripListener = new TripListener(network, stageActivityTypes, mainModeIdentifier, networkModes,
					new AvPersonAnalysisFilter());
			trips = new TripReaderFromEvents(tripListener).readTrips(eventsPath);
		} else {
			String populationPath = cmd.getOptionStrict("population-path");
			trips = new TripReaderFromPopulation(network, stageActivityTypes, mainModeIdentifier,
					new AvPersonAnalysisFilter()).readTrips(populationPath);
		}

		DistanceUnit inputUnit = DistanceUnit.valueOf(cmd.getOption("input-distance-unit").orElse("meter"));
		DistanceUnit outputUnit = DistanceUnit.valueOf(cmd.getOption("output-distance-unit").orElse("meter"));

		new TripWriter(trips, inputUnit, outputUnit).write(outputPath);
	}
}
