package org.eqasim.core.scenario.preparation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.CoordUtils;

public class AdjustLinkLength {
	private final Logger logger = Logger.getLogger(AdjustLinkLength.class);

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

			double directLength = CoordUtils.calcEuclideanDistance(link.getFromNode().getCoord(),
					link.getToNode().getCoord());

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
