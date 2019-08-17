package org.eqasim.switzerland.ovgk.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eqasim.switzerland.ovgk.OVGKConstants;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class RunWriteStopCategoryAsCSV {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("schedule-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		writer.write(String.join(";", new String[] { "x", "y", "category" }) + "\n");

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			writer.write(String.join(";", new String[] { //
					String.valueOf(facility.getCoord().getX()), //
					String.valueOf(facility.getCoord().getY()), //
					String.valueOf(facility.getAttributes().getAttribute(OVGKConstants.STOP_CATEGORY_ATTRIBUTE)) //
			}) + "\n");
		}

		writer.close();
	}
}
