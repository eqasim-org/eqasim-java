package org.eqasim.core.analysis.run;

import org.eqasim.core.analysis.pt.*;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.components.transit.events.PublicTransitEventMapper;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

import java.io.IOException;
import java.util.Collection;

public class RunPublicTransportStationUsageAnalysis {
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

		PublicTransportStationUsageListener tripListener = new PublicTransportStationUsageListener(scenario.getTransitSchedule());
		EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(tripListener);
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);
		reader.addCustomEventMapper(PublicTransitEvent.TYPE, new PublicTransitEventMapper());
		reader.readFile(eventsPath);
		new PublicTransportStationUsageWriter(tripListener.getUsagesMap().values()).write(outputPath);
	}
}
