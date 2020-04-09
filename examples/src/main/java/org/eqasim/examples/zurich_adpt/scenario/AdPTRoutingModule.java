package org.eqasim.examples.zurich_adpt.scenario;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eqasim.examples.zurich_adpt.mode_choice.costs.ZonalVariables;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zone;
import org.eqasim.examples.zurich_adpt.mode_choice.utilities.zones.Zones;
import org.locationtech.jts.geom.Coordinate;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.LinkWrapperFacility;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

public class AdPTRoutingModule implements RoutingModule {
	static final public String INTERACTION_ACTIVITY_TYPE = "adpt interaction";

	private final Zones zones;
	private final RoutingModule walkRoutingModule;
	private final RoutingModule roadRoutingModule;
	private final Network network;
	private final AdPTRouteFactory routeFactory;
	private final PopulationFactory populationFactory;
	private final ZonalVariables zonalVariables;
	public AdPTRoutingModule(RoutingModule walkRoutingModule, RoutingModule roadRoutingModule, Zones zones,
			Network network, AdPTRouteFactory routeFactory, PopulationFactory populationFactory, ZonalVariables zonalVariables) {
		this.walkRoutingModule = walkRoutingModule;
		this.roadRoutingModule = roadRoutingModule;
		this.zones = zones;
		this.network = network;
		this.routeFactory = routeFactory;
		this.populationFactory = populationFactory;
		this.zonalVariables = zonalVariables;
	}

	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility, Facility toFacility, double departureTime,
			Person person) {
		List<PlanElement> routeElements = new LinkedList<>();

		Coord startCoord = fromFacility.getCoord();

		Coord endCoord = toFacility.getCoord();

		String startZoneId = null;
		String endZoneId = null;
		Map<String, Zone> mapZones = this.zones.getZones();
		boolean foundStart = false;
		boolean foundEnd = false;
		for (Zone zone : mapZones.values()) {

			if (zone.containsCoordinate(startCoord)) {
				startZoneId = zone.code;
				foundStart = true;
			}
			if (zone.containsCoordinate(endCoord)) {
				endZoneId = zone.code;
				foundEnd = true;
			}
			if (foundStart && foundEnd)
				break;
		}

		Coordinate coordinateStopStart = mapZones.get(startZoneId).geometry.getCentroid().getCoordinate();
		Coord coordStopStart = CoordUtils.createCoord(coordinateStopStart);

		Coordinate coordinateStopEnd = mapZones.get(endZoneId).geometry.getCentroid().getCoordinate();
		Coord coordStopEnd = CoordUtils.createCoord(coordinateStopEnd);

		Facility pickupFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, coordStopStart));

		Facility dropoffFacility = new LinkWrapperFacility(NetworkUtils.getNearestLink(network, coordStopEnd));

		List<? extends PlanElement> accessElements = this.walkRoutingModule.calcRoute(fromFacility, pickupFacility,
				departureTime, null);

		routeElements.addAll(accessElements);
		double arrivalAtPickup = departureTime + ((Leg)accessElements.get(0)).getTravelTime();
		double headway = this.zonalVariables.getZoneZoneFrequency().get(startZoneId).get(endZoneId);
		double waitTime = (arrivalAtPickup % headway);
		Activity pickupActivity = populationFactory.createActivityFromLinkId(INTERACTION_ACTIVITY_TYPE,
				pickupFacility.getLinkId());
		pickupActivity.setMaximumDuration(waitTime);
		routeElements.add(pickupActivity);

		
		List<? extends PlanElement> transitElements = roadRoutingModule.calcRoute(pickupFacility, dropoffFacility,
				departureTime, null);
		Leg leg = (Leg) transitElements.get(0);
		double vehicleTravelTime = leg.getRoute().getTravelTime();

		//create AdPT route
		AdPTRoute route = routeFactory.createRoute(pickupFacility.getLinkId(), dropoffFacility.getLinkId());
		route.setInVehicleTime(vehicleTravelTime);
		route.setDistance(leg.getRoute().getDistance());
		leg = populationFactory.createLeg("adpt");
		leg.setTravelTime(vehicleTravelTime);
		leg.setRoute(route);

		routeElements.add(leg);
		List<? extends PlanElement> egressElements = this.walkRoutingModule.calcRoute(pickupFacility, toFacility,
				departureTime, null);

		routeElements.addAll(egressElements);

		return routeElements;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl(INTERACTION_ACTIVITY_TYPE);
	}

}
