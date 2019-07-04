package org.eqasim.scenario.cutter.network;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AdjustLinkLength {
	private final Logger logger = Logger.getLogger(AdjustLinkLength.class);

	public void run(Network network) {
		for (Link link : network.getLinks().values()) {
			double originalLength = link.getLength();
			double travelTime = originalLength / link.getFreespeed();

			if (travelTime < 1.0) {
				double updatedLength = link.getFreespeed();
				link.setLength(updatedLength);

				logger.info(String.format("Increasing length of link %s from %f to %f", link.getId().toString(),
						originalLength, updatedLength));
			}
		}
	}
}
