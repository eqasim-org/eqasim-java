package org.eqasim.scenario.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AdjustLinkLength {
	private final Logger logger = Logger.getLogger(AdjustLinkLength.class);

	public void run(Network network) {
		int infiniteSpeedCount = 0;

		for (Link link : network.getLinks().values()) {
			double originalLength = link.getLength();
			double originalFreespeed = link.getFreespeed();

			double updatedFreespeed = originalFreespeed;

			if (!Double.isInfinite(originalFreespeed)) {
				updatedFreespeed = 85.0;
				infiniteSpeedCount++;
			}

			double travelTime = originalLength / updatedFreespeed;

			if (travelTime < 1.0) {
				double updatedLength = updatedFreespeed;
				link.setLength(updatedLength);

				logger.info(String.format("Increasing length of link %s from %f to %f", link.getId().toString(),
						originalLength, updatedLength));
			}
		}

		logger.info(
				String.format("Set freespeed of %d links to 85.0, which as originally infinite", infiniteSpeedCount));
	}
}
