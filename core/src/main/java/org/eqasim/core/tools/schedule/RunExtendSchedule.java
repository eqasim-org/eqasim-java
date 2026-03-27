package org.eqasim.core.tools.schedule;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitScheduleWriter;
import org.matsim.vehicles.MatsimVehicleReader;
import org.matsim.vehicles.MatsimVehicleWriter;

public class RunExtendSchedule {
	static public void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-schedule-path", "output-schedule-path", "input-vehicles-path",
						"output-vehicles-path") //
				.allowOptions("end-time", "hours", "days") //
				.build();

		boolean endTimeAvailable = cmd.hasOption("end-time");
		boolean hoursAvailable = cmd.hasOption("hours");
		boolean daysAvailable = cmd.hasOption("days");

		if (!(endTimeAvailable ^ (hoursAvailable || daysAvailable))) {
			throw new IllegalStateException(
					"Either --end-time (in seconds), or --hours / --days (can be combined) need to be provided.");
		}

		double endTime = 0.0;
		if (endTimeAvailable) {
			endTime = Double.parseDouble(cmd.getOptionStrict("end-time"));
		} else {
			if (hoursAvailable) {
				endTime += 3600.0 * Double.parseDouble(cmd.getOptionStrict("hours"));
			}

			if (daysAvailable) {
				endTime += 24.0 * 3600.0 * Double.parseDouble(cmd.getOptionStrict("hours"));
			}
		}

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("input-schedule-path"));
		new MatsimVehicleReader(scenario.getTransitVehicles()).readFile(cmd.getOptionStrict("input-vehicles-path"));

		new ExtendSchedule(endTime).process(scenario.getTransitSchedule(), scenario.getTransitVehicles());

		new TransitScheduleWriter(scenario.getTransitSchedule()).writeFile(cmd.getOptionStrict("output-schedule-path"));
		new MatsimVehicleWriter(scenario.getTransitVehicles()).writeFile(cmd.getOptionStrict("output-vehicles-path"));
	}
}