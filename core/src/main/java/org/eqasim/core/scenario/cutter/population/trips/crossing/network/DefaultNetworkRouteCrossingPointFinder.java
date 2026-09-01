package org.eqasim.core.scenario.cutter.population.trips.crossing.network;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eqasim.core.scenario.cutter.extent.LinkRegionClassifier;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingData;
import org.eqasim.core.scenario.cutter.population.trips.crossing.network.timing.LinkTimingRegistry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

public class DefaultNetworkRouteCrossingPointFinder implements NetworkRouteCrossingPointFinder {
	final private Network network;

	final private Map<String, TravelTime> travelTimes;
	final private LinkTimingRegistry timingRegistry;

	@Inject
	public DefaultNetworkRouteCrossingPointFinder(Network network,
			Map<String, TravelTime> travelTimes, LinkTimingRegistry timingRegistry) {
		this.network = network;
		this.travelTimes = travelTimes;
		this.timingRegistry = timingRegistry;
	}

	@Override
	public List<NetworkRouteCrossingPoint> findCrossingPoints(Id<Person> personId, int legIndex, String mode,
			NetworkRoute route, double departureTime) {
		List<NetworkRouteCrossingPoint> crossingPoints = new LinkedList<>();

		List<Id<Link>> fullRoute = new LinkedList<>();
		fullRoute.add(route.getStartLinkId());
		fullRoute.addAll(route.getLinkIds());
		fullRoute.add(route.getEndLinkId());

		Link link = null;
		double enterTime = departureTime;
		double leaveTime = departureTime;

		int index = 0;

		TravelTime travelTime = travelTimes.get(mode);

		for (Id<Link> linkId : fullRoute) {
			link = network.getLinks().get(linkId);
			enterTime = leaveTime;
			leaveTime = enterTime + travelTime.getLinkTravelTime(link, enterTime, null, null);

			if (LinkRegionClassifier.isCrossing(link)) {
				boolean fromIsInside = LinkRegionClassifier.isFromNodeInside(link);
				Optional<LinkTimingData> timingData = timingRegistry.getTimingData(personId, legIndex, linkId);

				if (timingData.isPresent()) {
					enterTime = timingData.get().enterTime;
					leaveTime = timingData.get().leaveTime;
				}

				crossingPoints.add(new NetworkRouteCrossingPoint(index, link, enterTime, leaveTime, fromIsInside));
			}

			index++;
		}

		return crossingPoints;
	}

	@Override
	public boolean isInside(NetworkRoute route) {
		List<Id<Link>> fullRoute = new LinkedList<>();

		fullRoute.add(route.getStartLinkId());
		fullRoute.addAll(route.getLinkIds());
		fullRoute.add(route.getEndLinkId());

		for (Id<Link> linkId : fullRoute) {
			Link link = network.getLinks().get(linkId);

			if (!LinkRegionClassifier.isInside(link)) {
				return false;
			}
		}

		return true;
	}
}
