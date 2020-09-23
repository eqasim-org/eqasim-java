package org.eqasim.san_francisco.scenario;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class NetworkFixer {
	public void run(Network network) {
		for (Link link : network.getLinks().values()) {
			if (!Float.isFinite((float) (link.getFreespeed()))) {
				link.setFreespeed(20.0);
			}
		}
	}
}
