package org.eqasim.ile_de_france.policies;

import java.util.Random;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RunImputeCarLocation {
    public static void main(String[] args) throws ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("input-path", "output-path") //
				.build();

        Random random = new Random();

		// Load population
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		PopulationReader populationReader = new PopulationReader(scenario);
		populationReader.readFile(cmd.getOptionStrict("input-path"));

		for (Person person : scenario.getPopulation().getPersons().values()) {
			if (!person.getAttributes().getAttribute("carAvailability").equals("none")){
				boolean carAvailability = random.nextBoolean();
				person.getAttributes().putAttribute("carLocation", carAvailability ? "home" : "elsewhere");

			}
        }

		// Write population
		PopulationWriter populationWriter = new PopulationWriter(scenario.getPopulation());
		populationWriter.write(cmd.getOptionStrict("output-path"));
	}
}
