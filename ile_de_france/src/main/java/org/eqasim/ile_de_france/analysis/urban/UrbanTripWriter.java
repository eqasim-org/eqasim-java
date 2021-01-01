package org.eqasim.ile_de_france.analysis.urban;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

import org.eqasim.ile_de_france.mode_choice.utilities.predictors.IDFPredictorUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.CommandLine.ConfigurationException;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.pt.PtConstants;

public class UrbanTripWriter {
	private Population population;
	private StageActivityTypes stageActivityTypes;

	public UrbanTripWriter(Population population, StageActivityTypes stageActivityTypes) {
		this.population = population;
		this.stageActivityTypes = stageActivityTypes;
	}

	public void write(String outputPath) throws IOException {
		BufferedWriter writer = IOUtils.getBufferedWriter(outputPath);
		writer.write(String.join(";", new String[] { "person_id", "person_trip_id", "urban_origin", "urban_destination" })
				+ "\n");

		for (Person person : population.getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			List<Activity> activities = TripStructureUtils.getActivities(plan, stageActivityTypes);

			for (int i = 0; i < activities.size() - 1; i++) {
				Activity originActivity = activities.get(i);
				Activity destinationActivity = activities.get(i + 1);

				boolean hasUrbanOrigin = IDFPredictorUtils.isUrbanArea(originActivity);
				boolean hasUrbanDestination = IDFPredictorUtils.isUrbanArea(destinationActivity);

				writer.write(String.join(";", new String[] { person.getId().toString(), String.valueOf(i),
						String.valueOf(hasUrbanOrigin), String.valueOf(hasUrbanDestination) }) + "\n");
			}
		}

		writer.close();
	}

	static public void main(String[] args) throws IOException, ConfigurationException {
		CommandLine cmd = new CommandLine.Builder(args) //
				.requireOptions("population-path", "output-path") //
				.build();

		String populationPath = cmd.getOptionStrict("population-path");
		String outputPath = cmd.getOptionStrict("output-path");

		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		new PopulationReader(scenario).readFile(populationPath);

		Population population = scenario.getPopulation();
		StageActivityTypes stageActivityTypes = new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);

		new UrbanTripWriter(population, stageActivityTypes).write(outputPath);
	}
}
