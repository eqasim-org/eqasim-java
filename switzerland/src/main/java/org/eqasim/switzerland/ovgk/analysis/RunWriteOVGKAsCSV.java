package org.eqasim.switzerland.ovgk.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eqasim.switzerland.ovgk.OVGK;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunWriteOVGKAsCSV {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "schedule-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));

		List<Coord> coordinates = new LinkedList<>();

		String line = null;
		List<String> header = null;

		BufferedReader reader = new BufferedReader(
				new InputStreamReader(new FileInputStream(new File(cmd.getOptionStrict("input-path")))));

		while ((line = reader.readLine()) != null) {
			List<String> row = Arrays.asList(line.split(";"));

			if (header == null) {
				header = row;
			} else {
				coordinates.add(new Coord( //
						Double.parseDouble(row.get(header.indexOf("x"))), //
						Double.parseDouble(row.get(header.indexOf("y"))) //
				));
			}
		}

		reader.close();

		OVGKCalculator calculator = new OVGKCalculator(scenario.getTransitSchedule());

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		writer.write(String.join(";", new String[] { "x", "y", "category" }) + "\n");

		for (Coord coordinate : coordinates) {
			OVGK ovgk = calculator.calculateOVGK(coordinate);

			writer.write(String.join(";", new String[] { //
					String.valueOf(coordinate.getX()), //
					String.valueOf(coordinate.getY()), //
					String.valueOf(ovgk) //
			}) + "\n");
		}

		writer.close();
	}
}
