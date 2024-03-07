package org.eqasim.core.tools.schedule;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;

public class RunExtendSchedule {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "output-path", "period") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
		new ExtendSchedule(Double.parseDouble(cmd.getOptionStrict("period"))).process(scenario.getTransitSchedule());
		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(cmd.getOptionStrict("output-path"));
	}
}