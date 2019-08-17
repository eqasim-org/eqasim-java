package org.eqasim.switzerland.ovgk.analysis;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.eqasim.switzerland.ovgk.OVGK;
import org.eqasim.switzerland.ovgk.OVGKCalculator;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;

public class RunWriteHomeOVGKAsCSV {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "schedule-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new TransitScheduleReader(scenario).readFile(cmd.getOptionStrict("schedule-path"));
		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));

		OVGKCalculator calculator = new OVGKCalculator(scenario.getTransitSchedule());

		BufferedWriter writer = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(cmd.getOptionStrict("output-path"))));

		writer.write(String.join(";", new String[] { "x", "y", "category" }) + "\n");

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getType().equals("home")) {
						Coord coord = activity.getCoord();
						OVGK ovgk = calculator.calculateOVGK(coord);

						writer.write(String.join(";", new String[] { //
								String.valueOf(coord.getX()), //
								String.valueOf(coord.getY()), //
								String.valueOf(ovgk) //
						}) + "\n");

						break;
					}
				}
			}
		}

		writer.close();
	}
}
