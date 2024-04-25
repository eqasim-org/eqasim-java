package org.eqasim.server.services.router.transit;

import java.util.LinkedList;
import java.util.List;

import org.eqasim.core.components.raptor.EqasimRaptorConfigGroup;
import org.eqasim.core.components.raptor.EqasimRaptorUtils;
import org.eqasim.server.services.TransitConfiguration;
import org.eqasim.server.services.WalkConfiguration;
import org.eqasim.server.services.router.transit.TransitRouterResponse.Itinerary.ItineraryLeg.Type;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTWriter;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.common.base.Verify;

import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.DefaultRaptorTransferCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.LeastCostRaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorInVehicleCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorParameters;
import ch.sbb.matsim.routing.pt.raptor.RaptorRouteSelector;
import ch.sbb.matsim.routing.pt.raptor.RaptorStaticConfig;
import ch.sbb.matsim.routing.pt.raptor.RaptorStopFinder;
import ch.sbb.matsim.routing.pt.raptor.RaptorTransferCostCalculator;
import ch.sbb.matsim.routing.pt.raptor.RaptorUtils;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptor;
import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorData;
import jakarta.annotation.Nullable;

public class TransitRouterService {
	private final static GeometryFactory geometryFactory = new GeometryFactory();

	private final RaptorRouteSelector routeSelector = new LeastCostRaptorRouteSelector();
	private final RaptorInVehicleCostCalculator inVehicleCostCalculator = new DefaultRaptorInVehicleCostCalculator();
	private final RaptorTransferCostCalculator transferCostCalculator = new DefaultRaptorTransferCostCalculator();
	private final RaptorStopFinder stopFinder = new DefaultRaptorStopFinder(null, null);

	private final Config config;
	private final Network network;
	private final TransitSchedule schedule;

	private final SwissRailRaptorData data;
	private final TransitConfiguration configuration;
	private final WalkConfiguration walkConfiguration;

	TransitRouterService(SwissRailRaptorData data, Config config, Network network, TransitConfiguration configuration,
			WalkConfiguration walkConfiguration, TransitSchedule schedule) {
		this.data = data;
		this.configuration = configuration;
		this.config = config;
		this.network = network;
		this.walkConfiguration = walkConfiguration;
		this.schedule = schedule;
	}

	public TransitRouterResponse processRequest(TransitRouterRequest request, @Nullable TransitUtilities utilities) {
		TransitRouterResponse response = new TransitRouterResponse();
		response.requestIndex = request.requestIndex;

		if (request.provideItinerary) {
			response.itinerary = new TransitRouterResponse.Itinerary();
		}

		Coord fromCoord = new Coord(request.originX, request.originY);
		Coord toCoord = new Coord(request.destinationX, request.destinationY);

		Link fromLink = NetworkUtils.getNearestLink(network, fromCoord);
		Link toLink = NetworkUtils.getNearestLink(network, toCoord);

		Facility fromFacility = FacilitiesUtils.wrapLinkAndCoord(fromLink, fromCoord);
		Facility toFacility = FacilitiesUtils.wrapLinkAndCoord(toLink, toCoord);

		TransitUtilities appliedUtilities = mergeUtilities(utilities, request.utilities);

		RaptorParameters parameters = createParameters(config, configuration, walkConfiguration, appliedUtilities,
				schedule);
		SwissRailRaptor router = new SwissRailRaptor(data, person -> parameters, routeSelector, stopFinder,
				inVehicleCostCalculator, transferCostCalculator);

		List<? extends PlanElement> route = router.calcRoute(
				DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, request.departureTime_s, null));

		if (route == null) {
			double distance = CoordUtils.calcEuclideanDistance(fromCoord, toCoord);
			double travelTime = distance / parameters.getBeelineWalkSpeed();

			response.isOnlyWalk = true;
			response.transferWalkDistance_km = distance * 1e-3;
			response.transferWalkTime_min = travelTime / 60.0;
			response.totalTravelTime_min = travelTime / 60.0;
			response.arrivalTime_s = request.departureTime_s + travelTime;

			if (request.provideItinerary) {
				TransitRouterResponse.Itinerary.ItineraryLeg leg = new TransitRouterResponse.Itinerary.ItineraryLeg();
				response.itinerary.legs.add(leg);

				leg.type = Type.transfer;
				leg.mode = "walk";

				leg.departureTime_s = request.departureTime_s;
				leg.arrivalTime_s = request.departureTime_s + travelTime;
				leg.travelTime_min = travelTime / 60.0;
				leg.distance_km = distance * 1e-3;

				if (request.provideGeometry) {
					WKTWriter writer = new WKTWriter();

					Coordinate[] coordinates = new Coordinate[2];
					coordinates[0] = new Coordinate(fromCoord.getX(), fromCoord.getY());
					coordinates[1] = new Coordinate(toCoord.getX(), toCoord.getY());

					leg.geometry = writer.write(geometryFactory.createLineString(coordinates));
				}
			}
		} else if (route.size() == 1) {
			Leg routeLeg = (Leg) route.get(0);
			Verify.verify(routeLeg.getMode().equals("walk"));

			double distance = routeLeg.getRoute().getDistance();
			double travelTime = routeLeg.getRoute().getTravelTime().seconds();

			response.isOnlyWalk = true;
			response.transferWalkDistance_km = distance * 1e-3;
			response.transferWalkTime_min = travelTime / 60.0;
			response.totalTravelTime_min = travelTime / 60.0;
			response.arrivalTime_s = request.departureTime_s + travelTime;

			if (request.provideItinerary) {
				TransitRouterResponse.Itinerary.ItineraryLeg leg = new TransitRouterResponse.Itinerary.ItineraryLeg();
				response.itinerary.legs.add(leg);

				leg.type = Type.transfer;
				leg.mode = "walk";

				leg.departureTime_s = request.departureTime_s;
				leg.arrivalTime_s = request.departureTime_s + travelTime;
				leg.travelTime_min = travelTime / 60.0;
				leg.distance_km = distance * 1e-3;

				if (request.provideGeometry) {
					WKTWriter writer = new WKTWriter();

					Coordinate[] coordinates = new Coordinate[2];
					coordinates[0] = new Coordinate(fromCoord.getX(), fromCoord.getY());
					coordinates[1] = new Coordinate(toCoord.getX(), toCoord.getY());

					leg.geometry = writer.write(geometryFactory.createLineString(coordinates));
				}
			}
		} else {
			double currentTime = request.departureTime_s;

			int vehiclarLegs = 0;

			for (int elementIndex = 0; elementIndex < route.size(); elementIndex++) {
				Leg routeLeg = (Leg) route.get(elementIndex);

				if (routeLeg.getMode().equals("walk")) {
					double distance = routeLeg.getRoute().getDistance();
					double travelTime = routeLeg.getRoute().getTravelTime().seconds();

					response.totalTravelTime_min += travelTime / 60.0;

					final Type legType;

					if (elementIndex == 0) {
						response.accessWalkTime_min += travelTime / 60.0;
						response.accessWalkDistance_km += distance * 1e-3;
						legType = Type.access;
					} else if (elementIndex == route.size() - 1) {
						response.egressWalkTime_min += travelTime / 60.0;
						response.egressWalkDistance_km += distance * 1e-3;
						legType = Type.egress;
					} else {
						response.transferWalkTime_min += travelTime / 60.0;
						response.transferWalkDistance_km += distance * 1e-3;
						legType = Type.transfer;
					}

					if (request.provideItinerary) {
						TransitRouterResponse.Itinerary.ItineraryLeg leg = new TransitRouterResponse.Itinerary.ItineraryLeg();
						response.itinerary.legs.add(leg);

						leg.type = legType;
						leg.mode = "walk";

						leg.departureTime_s = currentTime;
						leg.arrivalTime_s = currentTime + travelTime;
						leg.travelTime_min = travelTime / 60.0;
						leg.distance_km = distance * 1e-3;

						if (request.provideGeometry) {
							WKTWriter writer = new WKTWriter();

							final Coordinate previousCoordinate;
							final Coordinate nextCoordinate;

							if (legType.equals(Type.access)) {
								previousCoordinate = new Coordinate(fromCoord.getX(), fromCoord.getY());

								TransitPassengerRoute nextRoute = (TransitPassengerRoute) ((Leg) route
										.get(elementIndex + 1)).getRoute();
								TransitStopFacility nextFacility = schedule.getFacilities()
										.get(nextRoute.getAccessStopId());
								nextCoordinate = new Coordinate(nextFacility.getCoord().getX(),
										nextFacility.getCoord().getY());
							} else if (legType.equals(Type.egress)) {
								TransitPassengerRoute previousRoute = (TransitPassengerRoute) ((Leg) route
										.get(elementIndex - 1)).getRoute();
								TransitStopFacility previousFacility = schedule.getFacilities()
										.get(previousRoute.getEgressStopId());
								previousCoordinate = new Coordinate(previousFacility.getCoord().getX(),
										previousFacility.getCoord().getY());

								nextCoordinate = new Coordinate(toCoord.getX(), toCoord.getY());
							} else { // transfer (but no only-walk)
								TransitPassengerRoute previousRoute = (TransitPassengerRoute) ((Leg) route
										.get(elementIndex - 1)).getRoute();
								TransitStopFacility previousFacility = schedule.getFacilities()
										.get(previousRoute.getEgressStopId());
								previousCoordinate = new Coordinate(previousFacility.getCoord().getX(),
										previousFacility.getCoord().getY());

								TransitPassengerRoute nextRoute = (TransitPassengerRoute) ((Leg) route
										.get(elementIndex + 1)).getRoute();
								TransitStopFacility nextFacility = schedule.getFacilities()
										.get(nextRoute.getAccessStopId());
								nextCoordinate = new Coordinate(nextFacility.getCoord().getX(),
										nextFacility.getCoord().getY());
							}

							Coordinate[] coordinates = new Coordinate[] { previousCoordinate, nextCoordinate };
							leg.geometry = writer.write(geometryFactory.createLineString(coordinates));
						}
					}

					currentTime += travelTime;
				} else { // vehicle
					TransitPassengerRoute passengerRoute = (TransitPassengerRoute) routeLeg.getRoute();
					vehiclarLegs++;

					TransitLine transitLine = schedule.getTransitLines().get(passengerRoute.getLineId());
					TransitRoute transitRoute = transitLine.getRoutes().get(passengerRoute.getRouteId());

					String mode = transitRoute.getTransportMode();
					double travelTime = passengerRoute.getTravelTime().seconds();
					double distance = passengerRoute.getDistance();

					double boardingTime = passengerRoute.getBoardingTime().seconds();
					double waitTime = boardingTime - currentTime;
					double inVehicleTime = travelTime - waitTime;

					response.totalTravelTime_min += travelTime / 60.0;
					response.inVehicleTravelTime_min += inVehicleTime / 60.0;
					response.inVehicleDistance_km += distance * 1e-3;

					if (vehiclarLegs == 1) {
						response.initialWaitTime_min += waitTime / 60.0;
					} else {
						response.transferWaitTime_min += waitTime / 60.0;
					}

					if (!response.inVehicleTravelTimeByMode_min.containsKey(mode)) {
						response.inVehicleTravelTimeByMode_min.put(mode, 0.0);
						response.inVehicleDistanceByMode_km.put(mode, 0.0);
					}

					response.inVehicleTravelTimeByMode_min.compute(mode, (m, v) -> v + inVehicleTime / 60.0);
					response.inVehicleDistanceByMode_km.compute(mode, (m, v) -> v + distance * 1e-3);

					if (request.provideItinerary) {
						TransitRouterResponse.Itinerary.ItineraryLeg leg = new TransitRouterResponse.Itinerary.ItineraryLeg();
						response.itinerary.legs.add(leg);

						leg.type = Type.vehicle;
						leg.mode = mode;

						leg.departureTime_s = currentTime;
						leg.arrivalTime_s = currentTime + travelTime;
						leg.travelTime_min = travelTime / 60.0;
						leg.distance_km = distance * 1e-3;

						leg.inVehicleTime_min = inVehicleTime / 60.0;
						leg.waitTime_min = waitTime / 60.0;

						leg.lineId = transitLine.getId().toString();
						leg.routeId = transitRoute.getId().toString();
						leg.lineName = transitLine.getName();

						if (request.provideGeometry) {
							Id<Link> startLinkId = passengerRoute.getStartLinkId();
							Id<Link> endLinkId = passengerRoute.getEndLinkId();

							NetworkRoute networkRoute = transitRoute.getRoute().getSubRoute(startLinkId, endLinkId);

							List<Coord> coords = new LinkedList<>();
							coords.add(network.getLinks().get(startLinkId).getFromNode().getCoord());

							for (Id<Link> linkId : networkRoute.getLinkIds()) {
								coords.add(network.getLinks().get(linkId).getFromNode().getCoord());
							}

							coords.add(network.getLinks().get(endLinkId).getToNode().getCoord());

							Coordinate[] coordinates = new Coordinate[coords.size()];

							for (int k = 0; k < coords.size(); k++) {
								coordinates[k] = new Coordinate(coords.get(k).getX(), coords.get(k).getY());
							}

							WKTWriter writer = new WKTWriter();
							leg.geometry = writer.write(geometryFactory.createLineString(coordinates));
						}

						if (request.provideItinerary) { // stop before
							TransitRouterResponse.Itinerary.ItineraryStop stop = new TransitRouterResponse.Itinerary.ItineraryStop();
							response.itinerary.stops.add(stop);

							TransitStopFacility stopFacility = schedule.getFacilities()
									.get(passengerRoute.getAccessStopId());

							stop.x = stopFacility.getCoord().getX();
							stop.y = stopFacility.getCoord().getY();

							stop.id = stopFacility.getId().toString();
							stop.name = stopFacility.getName();

							stop.arrivalTime_s = currentTime;
							stop.departureTime_s = passengerRoute.getBoardingTime().seconds();

							stop.waitTime_min = (stop.departureTime_s - stop.arrivalTime_s) / 60.0;

							if (request.provideGeometry) {
								WKTWriter writer = new WKTWriter();
								Coordinate coordinate = new Coordinate(stop.x, stop.y);
								stop.geometry = writer.write(geometryFactory.createPoint(coordinate));
							}
						}

						if (request.provideItinerary) { // stop after
							TransitRouterResponse.Itinerary.ItineraryStop stop = new TransitRouterResponse.Itinerary.ItineraryStop();
							response.itinerary.stops.add(stop);

							TransitStopFacility stopFacility = schedule.getFacilities()
									.get(passengerRoute.getEgressStopId());

							stop.x = stopFacility.getCoord().getX();
							stop.y = stopFacility.getCoord().getY();

							stop.id = stopFacility.getId().toString();
							stop.name = stopFacility.getName();

							stop.arrivalTime_s = currentTime;
							stop.departureTime_s = currentTime;
							stop.waitTime_min = 0.0;

							if (request.provideGeometry) {
								WKTWriter writer = new WKTWriter();
								Coordinate coordinate = new Coordinate(stop.x, stop.y);
								stop.geometry = writer.write(geometryFactory.createPoint(coordinate));
							}
						}
					}

					currentTime += travelTime;
				}
			}

			response.arrivalTime_s = currentTime;
			response.transfers = Math.max(vehiclarLegs - 1, 0);
		}

		return response;
	}

	static public record WalkParameters(double beelineWalkFactor, double beelineWalkSpeed_m_s) {
	}

	static public WalkParameters createWalkParameters(Config config, WalkConfiguration configuration) {
		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);

		double beelineWalkFactor = staticConfig.getBeelineWalkDistanceFactor();
		double beelineWalkSpeed_m_s = staticConfig.getBeelineWalkSpeed();

		if (configuration.beelineWalkFactor != null) {
			beelineWalkFactor = configuration.beelineWalkFactor;
		}

		if (configuration.beelineWalkSpeed_m_s != null) {
			beelineWalkSpeed_m_s = configuration.beelineWalkSpeed_m_s;
		}

		return new WalkParameters(beelineWalkFactor, beelineWalkSpeed_m_s);
	}

	static private RaptorParameters createParameters(Config config, TransitConfiguration configuration,
			WalkConfiguration walkConfiguration, @Nullable TransitUtilities utilities, TransitSchedule schedule) {
		RaptorParameters parameters = RaptorUtils.createParameters(config);

		EqasimRaptorConfigGroup raptorConfig = (EqasimRaptorConfigGroup) config.getModules()
				.get(EqasimRaptorConfigGroup.GROUP_NAME);

		if (raptorConfig != null) {
			parameters = EqasimRaptorUtils.createParameters(config, raptorConfig, schedule);
		}

		if (configuration.directWalkFactor != null) {
			parameters.setDirectWalkFactor(configuration.directWalkFactor);
		}

		if (utilities != null) {
			parameters.setUseTransportModeUtilities(true);

			if (utilities.rail_u_h != null) {
				parameters.setMarginalUtilityOfTravelTime_utl_s("rail", utilities.rail_u_h / 3600.0);
			}

			if (utilities.subway_u_h != null) {
				parameters.setMarginalUtilityOfTravelTime_utl_s("subway", utilities.subway_u_h / 3600.0);
			}

			if (utilities.bus_u_h != null) {
				parameters.setMarginalUtilityOfTravelTime_utl_s("bus", utilities.bus_u_h / 3600.0);
			}

			if (utilities.tram_u_h != null) {
				parameters.setMarginalUtilityOfTravelTime_utl_s("tram", utilities.tram_u_h / 3600.0);
			}

			if (utilities.wait_u_h != null) {
				parameters.setMarginalUtilityOfWaitingPt_utl_s(utilities.wait_u_h / 3600.0);
			}

			if (utilities.walk_u_h != null) {
				parameters.setMarginalUtilityOfTravelTime_utl_s("walk", utilities.walk_u_h / 3600.0);
			}

			if (utilities.transfer_u != null) {
				parameters.setTransferPenaltyFixCostPerTransfer(-utilities.transfer_u);
			}
		}

		WalkParameters walkParameters = createWalkParameters(config, walkConfiguration);
		parameters.setBeelineWalkSpeed(walkParameters.beelineWalkSpeed_m_s / walkParameters.beelineWalkFactor);

		return parameters;
	}

	static private RaptorStaticConfig createStaticConfig(Config config, TransitConfiguration configuration,
			WalkConfiguration walkConfiguration) {
		RaptorStaticConfig staticConfig = RaptorUtils.createStaticConfig(config);

		if (configuration.maximumTransferDistance_km != null) {
			staticConfig.setBeelineWalkConnectionDistance(configuration.maximumTransferDistance_km * 1e3);
		}

		WalkParameters walkParameters = createWalkParameters(config, walkConfiguration);
		staticConfig.setBeelineWalkSpeed(walkParameters.beelineWalkSpeed_m_s);
		staticConfig.setBeelineWalkDistanceFactor(walkParameters.beelineWalkFactor);

		return staticConfig;
	}

	static public TransitRouterService create(Config config, Network network, TransitSchedule schedule,
			TransitConfiguration configuration, WalkConfiguration walkConfiguration) {
		RaptorStaticConfig staticConfig = createStaticConfig(config, configuration, walkConfiguration);
		SwissRailRaptorData data = SwissRailRaptorData.create(schedule, null, staticConfig, network, null);

		return new TransitRouterService(data, config, network, configuration, walkConfiguration, schedule);
	}

	static public TransitUtilities mergeUtilities(TransitUtilities globalUtilities, TransitUtilities requestUtilities) {
		if (globalUtilities == null && requestUtilities == null) {
			return null;
		}

		if (globalUtilities == null) {
			return requestUtilities;
		}

		TransitUtilities copy = new TransitUtilities();
		copy.rail_u_h = globalUtilities.rail_u_h;
		copy.subway_u_h = globalUtilities.subway_u_h;
		copy.bus_u_h = globalUtilities.bus_u_h;
		copy.tram_u_h = globalUtilities.tram_u_h;
		copy.other_u_h = globalUtilities.other_u_h;
		copy.wait_u_h = globalUtilities.wait_u_h;
		copy.walk_u_h = globalUtilities.walk_u_h;
		copy.transfer_u = globalUtilities.transfer_u;

		if (requestUtilities != null) {
			if (requestUtilities.rail_u_h != null) {
				copy.rail_u_h = requestUtilities.rail_u_h;
			}

			if (requestUtilities.subway_u_h != null) {
				copy.subway_u_h = requestUtilities.subway_u_h;
			}

			if (requestUtilities.bus_u_h != null) {
				copy.bus_u_h = requestUtilities.bus_u_h;
			}

			if (requestUtilities.tram_u_h != null) {
				copy.tram_u_h = requestUtilities.tram_u_h;
			}

			if (requestUtilities.other_u_h != null) {
				copy.other_u_h = requestUtilities.other_u_h;
			}

			if (requestUtilities.wait_u_h != null) {
				copy.wait_u_h = requestUtilities.wait_u_h;
			}

			if (requestUtilities.walk_u_h != null) {
				copy.walk_u_h = requestUtilities.walk_u_h;
			}

			if (requestUtilities.transfer_u != null) {
				copy.transfer_u = requestUtilities.transfer_u;
			}
		}

		return copy;
	}
}
