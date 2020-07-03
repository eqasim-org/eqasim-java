package org.eqasim.ile_de_france;

import org.eqasim.core.simulation.EqasimConfigurator;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;

public class IDFConfigurator extends EqasimConfigurator {
	static public void adjustScenario(Scenario scenario, double urbanCapacityFactor, double urbanDelay) {
		// TODO: Put this in a config file!

		EqasimConfigurator.adjustScenario(scenario);

		for (Link link : scenario.getNetwork().getLinks().values()) {
			boolean isUrban = (Boolean) link.getAttributes().getAsMap().getOrDefault("isUrban", false);

			if (isUrban) {
				link.setCapacity(link.getCapacity() * urbanCapacityFactor);

				double maximumCapacity = 0.0;

				for (Link downstreamLink : link.getToNode().getInLinks().values()) {
					maximumCapacity = Math.max(maximumCapacity, downstreamLink.getCapacity());
				}

				if (link.getCapacity() < maximumCapacity) {
					double travelTime = link.getLength() / link.getFreespeed() + urbanDelay;
					link.setFreespeed(link.getLength() / travelTime);
				}
			}
		}
	}
}
