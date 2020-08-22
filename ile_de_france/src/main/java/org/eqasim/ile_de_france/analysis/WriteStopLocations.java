package org.eqasim.ile_de_france.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

public class WriteStopLocations {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("input-path"));

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		writer.write(String.join(";", new String[] { //
				"stop_id", "x", "y" }) + "\n");

		for (TransitStopFacility facility : scenario.getTransitSchedule().getFacilities().values()) {
			writer.write(String.join(";", new String[] { //
					facility.getId().toString(), //
					String.valueOf(facility.getCoord().getX()), //
					String.valueOf(facility.getCoord().getY()) //
			}) + "\n");
		}

		writer.close();
	}
}
