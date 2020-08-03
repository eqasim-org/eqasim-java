package org.eqasim.ile_de_france.analysis;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

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

public class WriteLocations {
	static public void main(String[] args) throws ConfigurationException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

		String inputPath = cmd.getOptionStrict("input-path");
		String outputPath = cmd.getOptionStrict("output-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(inputPath);

		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputPath))));
		writer.write("person_id;x;y\n");

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					Activity activity = (Activity) element;

					if (activity.getType().equals("home")) {
						writer.write(String.join(";", new String[] { //
								person.getId().toString(), //
								String.valueOf(activity.getCoord().getX()), //
								String.valueOf(activity.getCoord().getY()) //
						}) + "\n");
					}
				}
			}
		}
		
		writer.close();
	}
}
