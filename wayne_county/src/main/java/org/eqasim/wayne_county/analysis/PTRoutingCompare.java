package org.eqasim.wayne_county.analysis;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;

public class PTRoutingCompare {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createMutableScenario(config);

		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		Config config_eqasim = ConfigUtils.createConfig();

		Scenario scenario_eqasim = ScenarioUtils.createMutableScenario(config_eqasim);

		PopulationReader popReader_eqasim = new PopulationReader(scenario_eqasim);
		popReader_eqasim.readFile(args[1]);
		int count = 0;
		int count_eqasim = 0;
		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt")) {
						count++;
						break;
					}
				}
			}
		}

		for (Person person : scenario_eqasim.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt")) {
						count_eqasim++;
						break;
					}
				}
			}
		}
		int count_eqasim_tr_walk = 0;
		for (Person person : scenario_eqasim.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();
			boolean has_tr_walk = false;
			boolean has_pt = false;
			for (PlanElement pe : plan.getPlanElements()) {

				if (pe instanceof Leg) {
					if (((Leg) pe).getMode().equals("pt")) {
						has_pt = true;						
					}
					if (((Leg) pe).getMode().equals("transit_walk")) {
						has_tr_walk = true;						
					}
				}
			}
			
			if (has_tr_walk && !has_pt)
				count_eqasim_tr_walk++;
		}
		
		System.out.println("Number of pt trips in standard MATSim: " + count);
		System.out.println("Number of pt trips in eqasim: " + count_eqasim);
		System.out.println("Number of transit walk legs in eqasim: " + count_eqasim_tr_walk);

	}

}
