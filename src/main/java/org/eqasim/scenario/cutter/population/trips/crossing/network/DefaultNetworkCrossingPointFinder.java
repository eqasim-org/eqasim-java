package org.eqasim.scenario.cutter.population.trips.crossing.network;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

public class DefaultNetworkCrossingPointFinder implements NetworkCrossingPointFinder {
	final private ScenarioExtent extent;
	final private Network network;
	final private TravelTime travelTime;

	@Inject
	public DefaultNetworkCrossingPointFinder(ScenarioExtent extent, Network network, TravelTime travelTime) {
		this.extent = extent;
		this.network = network;
		this.travelTime = travelTime;
	}

	@Override
	public List<NetworkCrossingPoint> findCrossingPoints(NetworkRoute route, double departureTime) {
		List<NetworkCrossingPoint> crossingPoints = new LinkedList<>();

		List<Id<Link>> fullRoute = new LinkedList<>();
		fullRoute.add(route.getStartLinkId());
		fullRoute.addAll(route.getLinkIds());
		fullRoute.add(route.getEndLinkId());

		Link link = null;
		double enterTime = departureTime;
		double leaveTime = departureTime;

		int index = 0;

		for (Id<Link> linkId : fullRoute) {
			link = network.getLinks().get(linkId);
			enterTime = leaveTime;
			leaveTime = enterTime + travelTime.getLinkTravelTime(link, enterTime, null, null);

			boolean fromIsInside = extent.isInside(link.getFromNode().getCoord());
			boolean toIsInside = extent.isInside(link.getToNode().getCoord());

			if (fromIsInside != toIsInside) {
				crossingPoints.add(new NetworkCrossingPoint(index, link, enterTime, leaveTime, fromIsInside));
			}

			index++;
		}

		return crossingPoints;
	}
}
