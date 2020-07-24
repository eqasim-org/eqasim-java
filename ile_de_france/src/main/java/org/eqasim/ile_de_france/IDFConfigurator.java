package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;

public class IDFConfigurator extends EqasimConfigurator {
	static public void checkUrbanAttributes(Scenario scenario) {
		boolean foundUrbanActivityAttribute = false;
		boolean foundUrbanLinkAttribute = false;

		for (Link link : scenario.getNetwork().getLinks().values()) {
			if (link.getAttributes().getAttribute("isUrban") != null) {
				foundUrbanLinkAttribute = true;
				break;
			}
		}

		for (Person person : scenario.getPopulation().getPersons().values()) {
			for (PlanElement element : person.getSelectedPlan().getPlanElements()) {
				if (element instanceof Activity) {
					if (element.getAttributes().getAttribute("isUrban") != null) {
						foundUrbanActivityAttribute = true;
					}
				}
			}
		}

		if (!foundUrbanActivityAttribute) {
			throw new IllegalStateException(
					"Could not find 'isUrban' attribute in population. Are you using an old version of the pipeline?");
		}

		if (!foundUrbanLinkAttribute) {
			throw new IllegalStateException(
					"Could not find 'isUrban' attribute in network. Are you using an old version of the pipeline?");
		}
	}
}
