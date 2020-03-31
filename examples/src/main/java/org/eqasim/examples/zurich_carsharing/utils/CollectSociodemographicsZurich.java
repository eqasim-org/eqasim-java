package org.eqasim.examples.zurich_carsharing.utils;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.households.Household;
import org.matsim.households.HouseholdsReaderV10;

public class CollectSociodemographicsZurich {

	public static void main(String[] args) throws IOException {
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		PopulationReader popReader = new PopulationReader(scenario);
		popReader.readFile(args[0]);

		HouseholdsReaderV10 housReader = new HouseholdsReaderV10(scenario.getHouseholds());
		housReader.readFile(args[1]);

		BufferedWriter writer = IOUtils.getBufferedWriter(args[2]);
		writer.write("id,gender,age,ptGA,ptVerbund,car,bike,employment,hhlIncome,hhlSize,license\n");
		for (Household h : scenario.getHouseholds().getHouseholds().values()) {
			for (Id<Person> idPerson : h.getMemberIds()) {

				Person person = scenario.getPopulation().getPersons().get(idPerson);

				Plan plan = person.getSelectedPlan();
				Coord homeCoord = null;
				Coord workCoord = null;
				for (PlanElement pe : plan.getPlanElements()) {

					if (pe instanceof Activity) {
						if (((Activity) pe).getType().equals("home"))
							homeCoord = ((Activity) pe).getCoord();
						if (((Activity) pe).getType().equals("work"))
							workCoord = ((Activity) pe).getCoord();
					}
				}
				if (homeCoord != null) {
					if (workCoord != null) {
						writer.write(person.getId().toString() + "," + homeCoord.getX() + "," + homeCoord.getY() + ","
								+ workCoord.getX() + "," + workCoord.getY() + ","
								+ person.getAttributes().getAttribute("sex") + ","
								+ person.getAttributes().getAttribute("age") + ","
								+ person.getAttributes().getAttribute("ptHasGA") + ","
								+ person.getAttributes().getAttribute("ptHasVerbund") + ","
								+ person.getAttributes().getAttribute("carAvail") + ","
								+ person.getAttributes().getAttribute("bikeAvailability") + ","
								+ person.getAttributes().getAttribute("employed") + "," + h.getIncome().getIncome()
								+ "," + h.getMemberIds().size() + ","
								+ person.getAttributes().getAttribute("hasLicense") + "\n");
					} else {
						writer.write(person.getId().toString() + "," + homeCoord.getX() + "," + homeCoord.getY() + ","
								+ "" + "," + "" + "," + person.getAttributes().getAttribute("sex") + ","
								+ person.getAttributes().getAttribute("age") + ","
								+ person.getAttributes().getAttribute("ptHasGA") + ","
								+ person.getAttributes().getAttribute("ptHasVerbund") + ","
								+ person.getAttributes().getAttribute("carAvail") + ","
								+ person.getAttributes().getAttribute("bikeAvailability") + ","
								+ person.getAttributes().getAttribute("employed") + "," + h.getIncome().getIncome()
								+ "," + h.getMemberIds().size() + ","
								+ person.getAttributes().getAttribute("hasLicense") + "\n");
					}
				}
			}

		}
		writer.flush();
		writer.close();
	}

}
