package org.eqasim.core.analysis.run;

import java.io.IOException;
import java.util.Collection;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eqasim.core.analysis.DefaultPersonAnalysisFilter;
import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.analysis.activities.ActivityItem;
import org.eqasim.core.analysis.activities.ActivityListener;
import org.eqasim.core.analysis.activities.ActivityReaderFromEvents;
import org.eqasim.core.analysis.activities.ActivityReaderFromPopulation;
import org.eqasim.core.analysis.activities.ActivityWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.MatsimFacilitiesReader;

public class RunActivityAnalysis {
	private final static Logger logger = LogManager.getLogger(RunActivityAnalysis.class);

	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("output-path") //
				.allowOptions("population-path", "events-path", "network-path", "facilities-path") //
				.build();

		run(cmd, new DefaultPersonAnalysisFilter());
	}

	public static void run(CommandLine cmd, PersonAnalysisFilter personAnalysisFilter)
			throws ConfigurationException, IOException {
		if (!(cmd.hasOption("population-path") ^ cmd.hasOption("events-path"))) {
			throw new IllegalStateException("Either population-path or events-path must be provided.");
		}

		if (cmd.hasOption("population-path") && !cmd.hasOption("network-path")) {
			logger.warn("Coordinates may be incosistent with events if no network is provided.");
		}

		if (cmd.hasOption("population-path") && !cmd.hasOption("facilities-path")) {
			logger.warn("Coordinates may be incosistent with events if no facilities are provided.");
		}

		String outputPath = cmd.getOptionStrict("output-path");

		Collection<ActivityItem> activities = null;

		if (cmd.hasOption("events-path")) {
			String eventsPath = cmd.getOptionStrict("events-path");
			ActivityListener activityListener = new ActivityListener(personAnalysisFilter);
			activities = new ActivityReaderFromEvents(activityListener).readActivities(eventsPath);
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
			activities = new ActivityReaderFromPopulation(personAnalysisFilter, Optional.ofNullable(network),
					Optional.ofNullable(facilities)).readActivities(populationPath);
		}

		new ActivityWriter(activities).write(outputPath);
	}
}
