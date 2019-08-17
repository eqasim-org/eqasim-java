package org.eqasim.switzerland.scenario;

import org.eqasim.switzerland.ovgk.CalculateStopCategories;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class RunCalculateStopCategories {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("input-path"));
		new CalculateStopCategories().run(scenario.getTransitSchedule());
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(cmd.getOptionStrict("output-path"));
	}
}
