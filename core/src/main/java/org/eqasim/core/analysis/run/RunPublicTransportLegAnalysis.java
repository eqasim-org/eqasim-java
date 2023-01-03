package org.eqasim.core.analysis.run;

import java.io.IOException;
import java.util.Collection;

import org.eqasim.core.analysis.pt.PublicTransportLegItem;
import org.eqasim.core.analysis.pt.PublicTransportLegListener;
import org.eqasim.core.analysis.pt.PublicTransportLegReader;
import org.eqasim.core.analysis.pt.PublicTransportLegWriter;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunPublicTransportLegAnalysis {
	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("events-path", "schedule-path", "output-path") //
				.build();

		String outputPath = cmd.getOptionStrict("output-path");
		String eventsPath = cmd.getOptionStrict("events-path");
		String schedulePath = cmd.getOptionStrict("schedule-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(schedulePath);

		PublicTransportLegListener tripListener = new PublicTransportLegListener(scenario.getTransitSchedule());
		PublicTransportLegReader reader = new PublicTransportLegReader(tripListener);
		Collection<PublicTransportLegItem> trips = reader.readTrips(eventsPath);

		new PublicTransportLegWriter(trips).write(outputPath);
	}
}
