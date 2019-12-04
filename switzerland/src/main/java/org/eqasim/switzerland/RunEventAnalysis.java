package org.eqasim.switzerland;

import org.eqasim.switzerland.congestion.ScenarioResultListener;
import org.eqasim.switzerland.congestion.ScenarioResultWriter;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.scenario.ScenarioUtils;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class RunEventAnalysis {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("config-path", "event-dir", "output")
				.build();

		Config config = ConfigUtils.loadConfig(cmd.getOptionStrict("config-path"),
				SwitzerlandConfigurator.getConfigGroups());
		cmd.applyConfiguration(config);

		Scenario scenario = ScenarioUtils.createScenario(config);

		SwitzerlandConfigurator.configureScenario(scenario);
		ScenarioUtils.loadScenario(scenario);
		SwitzerlandConfigurator.adjustScenario(scenario);

		EventsManagerImpl eventsManager = new EventsManagerImpl();
		MatsimEventsReader reader = new MatsimEventsReader(eventsManager);

		List<Id<Link>> linkIds = new LinkedList<>();
		linkIds.add(Id.createLinkId("602433"));

		ScenarioResultListener listener = new ScenarioResultListener(scenario.getNetwork(), linkIds, 900);

		eventsManager.addHandler(listener);

		for (int i = 0; i<=40; i++) {
			listener.startIteration(i);
			reader.readFile(cmd.getOptionStrict("event-dir") + "/ITERS/it." + i + "/" +  i + ".events.xml.gz");
			listener.endIteration(i);
		}

		new ScenarioResultWriter(listener.getResults()).write(cmd.getOptionStrict("output"));

	}
}
