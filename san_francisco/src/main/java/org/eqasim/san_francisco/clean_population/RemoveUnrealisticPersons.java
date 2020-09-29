package org.eqasim.san_francisco.clean_population;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class RemoveUnrealisticPersons {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		Scenario scenario2 = ScenarioUtils.createScenario(config);

		int counter = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();
			boolean remove = false;
			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Leg) {

					if (((Leg) pe).getMode().equals("walk")) {
						if (((Leg) pe).getTravelTime().seconds() > 5400.0)
							remove = true;
					}

					if (((Leg) pe).getMode().equals("transit_walk")) {
						if (((Leg) pe).getTravelTime().seconds() > 5400.0)
							remove = true;
					}

				}
			}

			if (!remove) {
				scenario2.getPopulation().addPerson(person);
			} else
				counter++;
		}

		PopulationWriter popWriter = new PopulationWriter(scenario2.getPopulation());
		popWriter.write(args[1]);
		System.out.println("Removed " + counter + " persons.");

		System.out.println("Removed " + (double) counter / scenario.getPopulation().getPersons().values().size()
				+ " % of the population");
	}

}
