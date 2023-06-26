package org.eqasim.examples.corsica_carpooling;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class CarpoolingConfigurator {
	public void configureNetwork(Scenario scenario) {
		Network network = scenario.getNetwork();

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				Set<String> modes = new HashSet<>(link.getAllowedModes());
				modes.add("carpooling");
				link.setAllowedModes(modes);
			}
		}
	}
}
