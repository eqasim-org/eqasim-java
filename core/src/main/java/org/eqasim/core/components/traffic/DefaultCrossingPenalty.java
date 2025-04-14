package org.eqasim.core.components.traffic;

import org.matsim.api.core.v01.IdSet;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

public class DefaultCrossingPenalty implements CrossingPenalty {
	static public final String MAJOR_CROSSING_ATTRIBUTE = "eqasim:majorCrossing";

	private final IdSet<Link> penalizedLinkIds;
	private final double crossingPenalty;

	DefaultCrossingPenalty(IdSet<Link> penalizedLinkIds, double crossingPenalty) {
		this.penalizedLinkIds = penalizedLinkIds;
		this.crossingPenalty = crossingPenalty;
	}

	@Override
	public double calculateCrossingPenalty(Link link) {
		if (penalizedLinkIds.contains(link.getId())) {
			return crossingPenalty;
		} else {
			return 0.0;
		}
	}

	static public DefaultCrossingPenalty build(Network network, double crossingPenalty) {
		IdSet<Link> penalizedLinkIds = new IdSet<>(Link.class);

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains(TransportMode.car)) {
				if (link.getToNode().getInLinks().size() > 1) { // otherwise straight road or diverge
					double maximumCapacity = Double.NEGATIVE_INFINITY;
					boolean foundLower = false;

					for (Link inlink : link.getToNode().getInLinks().values()) {
						if (inlink.getCapacity() > maximumCapacity) {
							maximumCapacity = inlink.getCapacity();
						}

						foundLower |= inlink.getCapacity() < link.getCapacity();
					}

					if (link.getCapacity() == maximumCapacity && foundLower) {
						penalizedLinkIds.add(link.getId());
					}
				}
			}
		}

		return new DefaultCrossingPenalty(penalizedLinkIds, crossingPenalty);
	}
}
