package org.eqasim.sao_paulo.analysis;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;

public class CollectSociodemographicsFromPopulation {

	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);
		BufferedWriter writer = IOUtils.getBufferedWriter(args[1]);
		writer.write("id,gender,age,pt,car,license\n");
		for (Person person : scenario.getPopulation().getPersons().values()) {

			writer.write(person.getId().toString() + "," + person.getAttributes().getAttribute("sex") + ","
					+ person.getAttributes().getAttribute("age") + ","
					+ person.getAttributes().getAttribute("ptSubscription") + ","
					+ person.getAttributes().getAttribute("carAvailability") + ","
					+ person.getAttributes().getAttribute("hasLicense") + "\n");
		}

		writer.flush();
		writer.close();

	}

}