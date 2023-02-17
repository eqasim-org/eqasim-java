package org.eqasim.core.analysis.run;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.DistanceUnit;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.trips.TripItem;
import org.eqasim.core.analysis.trips.TripListener;
import org.eqasim.core.analysis.trips.TripReaderFromEvents;
import org.eqasim.core.analysis.trips.TripReaderFromPopulation;
import org.eqasim.core.analysis.trips.TripWriter;
import org.eqasim.core.components.EqasimMainModeIdentifier;
import org.eqasim.core.scenario.cutter.extent.ShapeScenarioExtent;
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
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunTripAnalysis {
	private final static Logger logger = Logger.getLogger(RunTripAnalysis.class);

	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path") //
				.allowOptions("population-path", "events-path", "network-path", "facilities-path") //
				.allowOptions("vehicle-modes") //
				.allowOptions("input-distance-units", "output-distance-units")
				.allowOptions("extent-path", "extent-attribute", "extent-value", "schedule-path")
				.allowOptions("main-mode-identifier")
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

		if (cmd.hasOption("extent-path") && !cmd.hasOption("events-path")) {
			logger.warn("For now the extent-path argument is only operational when the events-path argument is used");
		}

		if(cmd.hasOption("events-path") && (cmd.hasOption("extent-path") || cmd.hasOption("schedule-path")) && !(cmd.hasOption("extent-path") && cmd.hasOption("schedule-path"))) {
			throw new IllegalStateException("extent-path and schedule-path arguments should be both present if one of them is present and events-path is given");
		}

		String outputPath = cmd.getOptionStrict("output-path");

		MainModeIdentifier mainModeIdentifier;
		if(cmd.hasOption("main-mode-identifier")) {
			try {
				Class<?> mainModeIdentifierClass = Class.forName(cmd.getOptionStrict("main-mode-identifier"));
				boolean foundMainModeIdentifierInterface = false;
				for(Class<?> classObject : mainModeIdentifierClass.getInterfaces()) {
					if(classObject.equals(MainModeIdentifier.class)) {
						foundMainModeIdentifierInterface = true;
						break;
					}
				}
				if(!foundMainModeIdentifierInterface) {
					throw new IllegalStateException("The provided class " + cmd.getOptionStrict("main-mode-identifier") + " does not implement the MainModeIdentifier interface");
				}
				mainModeIdentifier = (MainModeIdentifier) mainModeIdentifierClass.getConstructor().newInstance();
			} catch (InstantiationException | IllegalAccessException | InvocationTargetException |
					 NoSuchMethodException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}else {
			mainModeIdentifier = new EqasimMainModeIdentifier();
		}

		Collection<String> vehicleModes = Arrays.asList(cmd.getOption("vehicle-modes").orElse("car,pt").split(","))
				.stream().map(s -> s.trim()).collect(Collectors.toSet());

		Collection<TripItem> trips;

		if (cmd.hasOption("events-path")) {
			String networkPath = cmd.getOptionStrict("network-path");
			Network network = NetworkUtils.createNetwork();
			new MatsimNetworkReader(network).readFile(networkPath);

			String eventsPath = cmd.getOptionStrict("events-path");
			ShapeScenarioExtent shapeScenarioExtent = null;
			TransitSchedule schedule = null;
			if (cmd.hasOption("extent-path")) {
				shapeScenarioExtent = new ShapeScenarioExtent.Builder(new File(cmd.getOptionStrict("extent-path")), cmd.getOption("extent-attribute"), cmd.getOption("extent-value")).build();
				Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
				new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
				schedule = scenario.getTransitSchedule();
			}
			TripListener tripListener = new TripListener(network, mainModeIdentifier, personAnalysisFilter, shapeScenarioExtent, schedule);
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
