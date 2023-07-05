package org.eqasim.server.backend.routing;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eqasim.server.backend.BackendScenario;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import ch.sbb.matsim.config.SwissRailRaptorConfigGroup;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorTransferCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.OccupancyData;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorRoute;
import ch.sbb.matsim.routing.pt.raptor.RaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig.RaptorOptimization;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorTransferCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;

public class TransitRouterBackend {
	private final QuadTree<TransitStopFacility> spatialIndex;

	private final Set<String> transitModes;
	private final SwissRailRaptorData data;

	TransitRouterBackend(SwissRailRaptorData data, Set<String> transitModes,
			QuadTree<TransitStopFacility> spatialIndex) {
		this.data = data;
		this.transitModes = transitModes;
		this.spatialIndex = spatialIndex;
	}

	public static TransitRouterBackend create(BackendScenario scenario) {
		TransitSchedule schedule = scenario.getSchedule();
		Network network = scenario.getNetwork();

		RaptorStaticConfig staticConfig = new RaptorStaticConfig();
		staticConfig.setOptimization(RaptorOptimization.OneToAllRouting);
		staticConfig.setBeelineWalkConnectionDistance(1000.0);
		staticConfig.setBeelineWalkDistanceFactor(1.3);
		staticConfig.setBeelineWalkSpeed(1.3);

		OccupancyData occupancyData = new OccupancyData();
		SwissRailRaptorData data = SwissRailRaptorData.create(schedule, null, staticConfig, network, occupancyData);

		Set<String> transitModes = schedule.getTransitLines().values().stream()
				.flatMap(item -> item.getRoutes().values().stream()).map(item -> item.getTransportMode())
				.collect(Collectors.toSet());

		QuadTree<TransitStopFacility> spatialIndex = QuadTrees.createQuadTree(schedule.getFacilities().values());

		return new TransitRouterBackend(data, transitModes, spatialIndex);
	}

	private final RaptorRouteSelector routeSelector = new LeastCostRaptorRouteSelector();
	private final RaptorInVehicleCostCalculator inVehicleCostCalculator = new DefaultRaptorInVehicleCostCalculator();
	private final RaptorTransferCostCalculator transferCostCalculator = new DefaultRaptorTransferCostCalculator();
	private final RaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, null);

	public SwissRailRaptor getInstance(Configuration routingConfiguration) {
		SwissRailRaptorConfigGroup config = new SwissRailRaptorConfigGroup();
		RaptorParameters parameters = new RaptorParameters(config);

		parameters.setDirectWalkFactor(routingConfiguration.directWalkFactor);
		parameters.setBeelineWalkSpeed(routingConfiguration.beelinkWalkSpeed / routingConfiguration.beelineWalkFactor);

		parameters.setMarginalUtilityOfWaitingPt_utl_s(routingConfiguration.waitingUtility);
		parameters.setTransferPenaltyFixCostPerTransfer(-routingConfiguration.transferUtility * 3600.0);

		for (String mode : transitModes) {
			parameters.setMarginalUtilityOfTravelTime_utl_s(mode, routingConfiguration.travelUtility);
		}

		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.pt, routingConfiguration.walkUtility);
		parameters.setMarginalUtilityOfTravelTime_utl_s(TransportMode.walk, routingConfiguration.walkUtility);

		return new SwissRailRaptor(data, person -> parameters, routeSelector, stopFinder, inVehicleCostCalculator,
				transferCostCalculator);
	}

	public Route route(Request request, Configuration configuration) {
		SwissRailRaptor raptor = getInstance(configuration);

		Coord fromCoord = new Coord(request.originX, request.originY);
		Coord toCoord = new Coord(request.destinationX, request.destinationY);

		Facility fromFacility = new FakeFacility(fromCoord);
		Facility toFacility = new FakeFacility(toCoord);

		List<RaptorRoute> routes = raptor.calcRoutes(fromFacility, toFacility, request.departureTime,
				request.departureTime, request.departureTime, null);

		if (routes.size() > 0) {
			Route data = new Route();
			data.valid = false;
			return data;
		} else {
			RaptorRoute route = routes.get(0);
			route.getParts().forEach(null);

			Route data = new Route();
			data.valid = true;
			data.transfers = route.getNumberOfTransfers();
			data.travelTime = route.getTravelTime();
			return data;
		}
	}

	static public class Configuration {
		public double directWalkFactor = 100.0;

		public double beelineWalkFactor = 1.3;
		public double beelinkWalkSpeed = 1.2;

		public double waitingUtility = -1.0;
		public double travelUtility = -1.0;
		public double transferUtility = -0.1;
		public double walkUtility = -1.0;
	}

	static public class Request {
		public double originX;
		public double originY;

		public double destinationX;
		public double destinationY;

		public double departureTime;
	}

	static public class Route {
		public boolean valid;
		public double travelTime;
		public int transfers;
	}
}
