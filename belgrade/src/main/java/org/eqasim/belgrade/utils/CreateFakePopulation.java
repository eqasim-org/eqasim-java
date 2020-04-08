package org.eqasim.belgrade.utils;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;

public class CreateFakePopulation {

	public static void main(String[] args) {

		Config config = ConfigUtils.createConfig();

		Scenario scenario = ScenarioUtils.createScenario(config);

		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario.getNetwork());

		netReader.readFile(args[0]);
		int id = 0;
		for (Link link : scenario.getNetwork().getLinks().values()) {

			if (link.getAttributes().getAttribute("osm:way:highway") != null
					&& link.getAttributes().getAttribute("osm:way:highway").equals("residential")) {
				for (int i = 0; i < 3; i++) {
					Person person = createPerson(link, scenario, id++);
					scenario.getPopulation().addPerson(person);
				}
			}
		}

		PopulationWriter popWriter = new PopulationWriter(scenario.getPopulation());
		popWriter.write(args[1]);

	}

	private static Person createPerson(Link link, Scenario scenario, int id) {

		double x1 = 5142068.0;
		double x2 = 5149354.0;

		double y1 = 2467393.0;
		double y2 = 2471140.0;

		Network network = scenario.getNetwork();
		double x = x1 + MatsimRandom.getRandom().nextDouble() * (x2 - x1);
		double y = y1 + MatsimRandom.getRandom().nextDouble() * (y2 - y1);
		Coord coord = CoordUtils.createCoord(x, y);
		Link linkDes = NetworkUtils.getNearestLink(network, coord);
		Plan plan = PopulationUtils.createPlan();
		Activity startAc = (PopulationUtils.createActivityFromCoord("home", link.getCoord()));
		startAc.setEndTime(7.0 * 3600.0 + 7200.0 * MatsimRandom.getRandom().nextDouble());
		plan.addActivity(startAc);
		plan.addLeg(PopulationUtils.createLeg("car"));
		plan.addActivity(PopulationUtils.createActivityFromCoord("work", linkDes.getCoord()));
		Person person = scenario.getPopulation().getFactory().createPerson(Id.createPersonId(id));
		person.addPlan(plan);
		return person;
	}

}
