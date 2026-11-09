package org.eqasim.core.scenario.preparation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class AdjustLinkLength {
	private final Logger logger = LogManager.getLogger(AdjustLinkLength.class);

	public void run(Network network) {
		int infiniteSpeedCount = 0;
		int adjustedLengthCount = 0;
		int minimumLengthCount = 0;

		for (Link link : network.getLinks().values()) {
			double originalLength = link.getLength();
			double originalFreespeed = link.getFreespeed();

			double updatedFreespeed = originalFreespeed;

			if (Double.isInfinite(originalFreespeed)) {
				updatedFreespeed = 85.0;
				infiniteSpeedCount++;
			}

			double travelTime = originalLength / updatedFreespeed;

			if (travelTime < 1.0) {
				double updatedLength = updatedFreespeed;
				link.setLength(updatedLength);
				adjustedLengthCount++;
			}

			// This is a horizontal-geometry sanity check. Missing elevations may be
			// encoded as zero outside the elevation coverage; interpreting that as
			// a real height would turn short border links into artificial climbs.
			// Keep the supplied road length (and the minimum travel-time rule above).
			var from = link.getFromNode().getCoord();
			var to = link.getToNode().getCoord();
			double directLength = Math.hypot(to.getX() - from.getX(), to.getY() - from.getY());

			if (link.getLength() < directLength) {
				minimumLengthCount++;
				link.setLength(directLength);
			}
		}

		logger.info(
				String.format("Set freespeed of %d links to 85.0, which as originally infinite", infiniteSpeedCount));
		logger.info(String.format("Adjusted length of %d links to speed", adjustedLengthCount));
		logger.info(String.format("Adjusted length of %d links to euclidean distance", minimumLengthCount));
	}
}
