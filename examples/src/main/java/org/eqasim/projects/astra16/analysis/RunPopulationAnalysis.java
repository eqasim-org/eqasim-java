package org.eqasim.projects.astra16.analysis;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

public class RunPopulationAnalysis {
	static public void main(String[] args)
			throws ConfigurationException, JsonGenerationException, JsonMappingException, IOException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "output-path") //
				.build();

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);

		new PopulationReader(scenario).readFile(cmd.getOptionStrict("population-path"));
		List<Item> items = new LinkedList<>();

		for (Person person : scenario.getPopulation().getPersons().values()) {
			Item item = new Item();

			item.personId = person.getId().toString();
			item.isMale = ((String) person.getAttributes().getAttribute("sex")).equals("m");
			item.age = (Integer) person.getAttributes().getAttribute("age");
			item.householdIncome = (Double) person.getAttributes().getAttribute("householdIncome");
			item.hasCarTrip = false;

			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Leg) {
					if (((Leg) element).getMode().equals("car")) {
						item.hasCarTrip = true;
						break;
					}
				}
			}

			items.add(item);
		}

		CsvMapper mapper = new CsvMapper();
		CsvSchema schema = mapper.schemaFor(Item.class).withColumnSeparator(';').withHeader();

		// Write CSV
		File outputPath = new File(cmd.getOptionStrict("output-path"));
		mapper.writer(schema).writeValue(outputPath, items);
	}

	@SuppressWarnings("unused")
	private static class Item {
		public String personId;
		public boolean isMale;
		public boolean hasCarTrip;
		public int age;
		public double householdIncome;
	}
}
