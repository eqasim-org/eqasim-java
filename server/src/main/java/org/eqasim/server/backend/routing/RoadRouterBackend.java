package org.eqasim.server.backend.routing;

import org.eqasim.server.backend.BackendScenario;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.OnlyTimeDependentTravelDisutility;
import org.matsim.core.router.speedy.SpeedyALTFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.geometry.CoordUtils;

public class RoadRouterBackend {
	private final Network network;

	RoadRouterBackend(Network network) {
		this.network = network;
	}

	public static RoadRouterBackend create(BackendScenario scenario) {
		Network carNetwork = scenario.getCarNetwork();
		return new RoadRouterBackend(carNetwork);
	}

	private final TravelTime travelTime = new FreeSpeedTravelTime();
	private final TravelDisutility travelDisutility = new OnlyTimeDependentTravelDisutility(travelTime);

	private LeastCostPathCalculator getInstance() {
		return new SpeedyALTFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	public Route route(Request request, Configuration configuration) {
		LeastCostPathCalculator router = getInstance();

		Coord fromCoord = new Coord(request.originX, request.originY);
		Coord toCoord = new Coord(request.destinationX, request.destinationY);

		Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
		Link toLink = NetworkUtils.getNearestLink(network, toCoord);

		Node fromNode = fromLink.getToNode();
		Node toNode = toLink.getFromNode();

		double accessWalkDistance = CoordUtils.calcEuclideanDistance(fromCoord, fromLink.getCoord());
		double egressWalkDistance = CoordUtils.calcEuclideanDistance(toCoord, toLink.getCoord());

		double walkTravelTime = configuration.beelineWalkFactor * (accessWalkDistance + egressWalkDistance)
				/ configuration.beelinkWalkSpeed;

		Path path = router.calcLeastCostPath(fromNode, toNode, request.departureTime, null, null);

		Route route = new Route();
		route.accessEgressWalkTime = walkTravelTime;
		route.inVehicletravelTime = path.travelTime;
		route.totalTravelTime = walkTravelTime + path.travelTime;
		return route;
	}

	static public class Configuration {
		public double beelineWalkFactor = 1.3;
		public double beelinkWalkSpeed = 1.2;
	}

	static public class Request {
		public double originX;
		public double originY;

		public double destinationX;
		public double destinationY;

		public double departureTime;
	}

	static public class Route {
		public double accessEgressWalkTime;
		public double inVehicletravelTime;
		public double totalTravelTime;
	}
}
