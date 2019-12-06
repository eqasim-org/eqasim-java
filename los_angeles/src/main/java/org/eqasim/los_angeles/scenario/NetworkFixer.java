package org.eqasim.los_angeles.scenario;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class NetworkFixer {
	public void run(Network network) {
		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				Set<String> allowedModes = new HashSet<>(link.getAllowedModes());
				allowedModes.add("car_passenger");
				link.setAllowedModes(allowedModes);
			}
		}
	}
}
