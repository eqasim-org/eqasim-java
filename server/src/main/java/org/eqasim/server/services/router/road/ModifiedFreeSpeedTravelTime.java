package org.eqasim.server.services.router.road;

import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.vehicles.Vehicle;

public class ModifiedFreeSpeedTravelTime {
	private final TravelTime delegate = new FreeSpeedTravelTime();
	private final IdMap<Link, LinkRecord> links;

	private ModifiedFreeSpeedTravelTime(IdMap<Link, LinkRecord> links) {
		this.links = links;
	}

	public double getLinkTravelTime(FreespeedSettings settings, Link link, double time, Person person,
			Vehicle vehicle) {
		LinkRecord linkRecord = links.get(link.getId());

		double roadFactor = switch (linkRecord.linkType) {
		case major -> settings.majorFactor;
		case intermediate -> settings.intermediateFactor;
		case minor -> settings.minorFactor;
		default -> 1.0;
		};

		double crossingPenalty = switch (linkRecord.crossingType) {
		case major -> settings.majorCrossingPenalty_s;
		case minor -> settings.minorCrossingPenalty_s;
		default -> 0.0;
		};

		return delegate.getLinkTravelTime(link, time, person, vehicle) * roadFactor + crossingPenalty;
	}

	public enum LinkType {
		major, intermediate, minor
	}

	public enum CrossingType {
		major, minor, none
	}

	public record LinkRecord(LinkType linkType, CrossingType crossingType) {
	}

	static public ModifiedFreeSpeedTravelTime create(Network network) {
		IdMap<Link, LinkRecord> links = new IdMap<>(Link.class);

		for (Link link : network.getLinks().values()) {
			if (link.getAllowedModes().contains("car")) {
				links.put(link.getId(), new LinkRecord(decideLinkType(link), decideCrossingType(link)));
			}
		}

		return new ModifiedFreeSpeedTravelTime(links);
	}

	private static LinkType decideLinkType(Link link) {
		String osm = (String) link.getAttributes().getAttribute("osm:way:highway");

		if (osm != null) {
			if (osm.contains("motorway") || osm.contains("trunk") || osm.contains("primary")) {
				return LinkType.major;
			} else if (osm.contains("secondary") || osm.contains("tertiary")) {
				return LinkType.intermediate;
			}
		}

		return LinkType.minor;
	}

	private static CrossingType decideCrossingType(Link link) {
		if (link.getToNode().getInLinks().size() == 1 && link.getToNode().getOutLinks().size() == 1) {
			return CrossingType.none;
		} else {
			double maximumCapacity = Double.NEGATIVE_INFINITY;
			int maximumCount = 0;

			for (Link inlink : link.getToNode().getInLinks().values()) {

				if (inlink.getCapacity() > maximumCapacity) {
					maximumCapacity = inlink.getCapacity();
					maximumCount = 1;
				} else if (inlink.getCapacity() == maximumCapacity) {
					maximumCount++;
				}
			}

			if (maximumCapacity == link.getCapacity() && maximumCount == 2) {
				// highest capacity and only one other link coming in (from opposite direction)
				return CrossingType.major;
			} else {
				return CrossingType.minor;
			}
		}
	}
}
