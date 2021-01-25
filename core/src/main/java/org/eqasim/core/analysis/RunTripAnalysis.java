package org.eqasim.core.analysis;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eqasim.core.components.EqasimMainModeIdentifier;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.MatsimFacilitiesReader;

public class RunTripAnalysis {
	private final static Logger logger = Logger.getLogger(RunTripAnalysis.class);

	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path") //
				.allowOptions("population-path", "events-path", "network-path", "facilities-path") //
				.allowOptions("vehicle-modes") //
				.allowOptions("input-distance-units", "output-distance-units") //
				.build();

		run(cmd, new DefaultPersonAnalysisFilter());
	}

	public static void run(CommandLine cmd, PersonAnalysisFilter personAnalysisFilter)
			throws ConfigurationException, IOException {
		if (!(cmd.hasOption("population-path") ^ cmd.hasOption("events-path"))) {
			throw new IllegalStateException("Either population-path or events-path must be provided.");
		}

		if (cmd.hasOption("events-path") && !cmd.hasOption("network-path")) {
			throw new IllegalStateException("Network must be given for events analysis.");
		}

		if (cmd.hasOption("population-path") && !cmd.hasOption("network-path")) {
			logger.warn(
					"Coordinates and Euclidean distances may be incosistent with events if no network is provided.");
		}

		if (cmd.hasOption("population-path") && !cmd.hasOption("facilities-path")) {
			logger.warn(
					"Coordinates and Euclidean distances may be incosistent with events if no facilities are provided.");
		}

		String outputPath = cmd.getOptionStrict("output-path");

		MainModeIdentifier mainModeIdentifier = new EqasimMainModeIdentifier();

		Collection<String> vehicleModes = Arrays.asList(cmd.getOption("vehicle-modes").orElse("car,pt").split(","))
				.stream().map(s -> s.trim()).collect(Collectors.toSet());

		Collection<TripItem> trips = null;

		if (cmd.hasOption("events-path")) {
			String networkPath = cmd.getOptionStrict("network-path");
			Network network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(networkPath);

			String eventsPath = cmd.getOptionStrict("events-path");
			TripListener tripListener = new TripListener(network, mainModeIdentifier, personAnalysisFilter);
			trips = new TripReaderFromEvents(tripListener).readTrips(eventsPath);
		} else {
			Network network = null;
			ActivityFacilities facilities = null;

			if (cmd.hasOption("network-path")) {
				String networkPath = cmd.getOptionStrict("network-path");
				network = NetworkUtils.createNetwork();
				new MatsimNetworkReader(network).readFile(networkPath);
			}

			if (cmd.hasOption("facilities-path")) {
				String facilitiesPath = cmd.getOptionStrict("facilities-path");

				Config config = ConfigUtils.createConfig();
				Scenario scenario = ScenarioUtils.createScenario(config);
				new MatsimFacilitiesReader(scenario).readFile(facilitiesPath);

				facilities = scenario.getActivityFacilities();
			}

			String populationPath = cmd.getOptionStrict("population-path");
			trips = new TripReaderFromPopulation(vehicleModes, mainModeIdentifier, personAnalysisFilter,
					Optional.ofNullable(network), Optional.ofNullable(facilities)).readTrips(populationPath);
		}

		DistanceUnit inputUnit = DistanceUnit.valueOf(cmd.getOption("input-distance-unit").orElse("meter"));
		DistanceUnit outputUnit = DistanceUnit.valueOf(cmd.getOption("output-distance-unit").orElse("meter"));

		new TripWriter(trips, inputUnit, outputUnit).write(outputPath);
	}
}
