package org.eqasim.examples.zurich_parking.mode_choice.utilities.predictors;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.log4j.Logger;
import org.eqasim.core.simulation.mode_choice.cost.CostModel;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.CachedVariablePredictor;
import org.eqasim.core.simulation.mode_choice.utilities.predictors.PredictorUtils;
import org.eqasim.examples.zurich_parking.mode_choice.utilities.variables.ZurichParkingCarVariables;
import org.eqasim.examples.zurich_parking.parking.ParkingListener;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichBlueZoneParking;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichParkingGarage;
import org.eqasim.examples.zurich_parking.parking.manager.facilities.ZurichWhiteZoneParking;
import org.eqasim.switzerland.mode_choice.parameters.SwissCostParameters;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacility;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacilityType;
import org.matsim.contrib.parking.parkingsearch.routing.ParkingRouter;
import org.matsim.contribs.discrete_mode_choice.model.DiscreteModeChoiceTrip;
import org.matsim.contribs.discrete_mode_choice.model.utilities.MultinomialLogitSelector;
import org.matsim.contribs.discrete_mode_choice.model.utilities.UtilityCandidate;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;

import java.util.*;

public class ZurichParkingCarPredictor extends CachedVariablePredictor<ZurichParkingCarVariables> {
	private final CostModel costModel;
	private final ModeParameters parameters;
	private final SwissCostParameters costParameters;
	private final ParkingListener parkingListener;
	private final ParkingRouter router;
	private final Scenario scenario;

	private final Map<String, Map<Id<ActivityFacility>, ParkingFacility>> parkingFacilitiesByType = new HashMap<>();
	private Map<String, QuadTree<Id<ActivityFacility>>> parkingFacilityQuadTreeByType = null;

	private static final Logger log = Logger.getLogger(ActivityFacilitiesImpl.class);
	private final Random random = new Random(1);

	@Inject
	public ZurichParkingCarPredictor(ModeParameters parameters, @Named("car") CostModel costModel,
									 SwissCostParameters costParameters,
									 ParkingListener parkingListener,
									 Scenario scenario,
									 ParkingRouter router) {
		this.costModel = costModel;
		this.parameters = parameters;
		this.costParameters = costParameters;
		this.parkingListener = parkingListener;
		this.scenario = scenario;
		this.router = router;

		// populate garage facilities map
		for (ActivityFacility facility : scenario.getActivityFacilities().getFacilitiesForActivityType(ParkingUtils.PARKACTIVITYTYPE).values()) {

			Id<ActivityFacility> parkingId = facility.getId();
			Coord parkingCoord = facility.getCoord();
			Id<Link> parkingLinkId = facility.getLinkId();
			org.eqasim.examples.zurich_parking.parking.manager.facilities.ParkingFacilityType parkingFacilityType = org.eqasim.examples.zurich_parking.parking.manager.facilities.ParkingFacilityType.valueOf(facility.getAttributes().getAttribute("parkingFacilityType").toString());
			double maxParkingDuration = Double.parseDouble(facility.getAttributes().getAttribute("maxParkingDuration").toString());
			double parkingCapacity = facility.getActivityOptions().get(ParkingUtils.PARKACTIVITYTYPE).getCapacity();

			ParkingFacility parkingFacility;

			switch (parkingFacilityType) {
				case BlueZone:
					parkingFacility = new ZurichBlueZoneParking(parkingId, parkingCoord, parkingLinkId,
							parkingCapacity, maxParkingDuration);
					break;
				case LowTariffWhiteZone:
					parkingFacility = new ZurichWhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
							maxParkingDuration, org.eqasim.examples.zurich_parking.parking.manager.facilities.ParkingFacilityType.LowTariffWhiteZone.toString(), parkingCapacity);
					break;
				case HighTariffWhiteZone:
					parkingFacility = new ZurichWhiteZoneParking(parkingId, parkingCoord, parkingLinkId,
							maxParkingDuration, org.eqasim.examples.zurich_parking.parking.manager.facilities.ParkingFacilityType.HighTariffWhiteZone.toString(), parkingCapacity);
					break;
				case Garage:
					parkingFacility = new ZurichParkingGarage(parkingId, parkingCoord, parkingLinkId,
							parkingCapacity, maxParkingDuration);
					break;
				default:
					throw new IllegalStateException("Unexpected value: " + parkingFacilityType.toString());
			}

			this.parkingFacilitiesByType.putIfAbsent(parkingFacilityType.toString(), new HashMap<>());
			this.parkingFacilitiesByType.get(parkingFacilityType.toString()).putIfAbsent(facility.getId(), parkingFacility);
		}

		// build quadtree
		if (this.parkingFacilityQuadTreeByType == null) {
			this.buildQuadTree();
		}
	}

	synchronized private void buildQuadTree() {
		/* the method must be synchronized to ensure we only build one quadTree
		 * in case that multiple threads call a method that requires the quadTree.
		 */
		if (this.parkingFacilityQuadTreeByType != null) {
			return;
		}

		// get the range of the quadtrees to build based on network
		double startTime = System.currentTimeMillis();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node n : this.scenario.getNetwork().getNodes().values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15

		// build general quadtree by parking type
		{
			this.parkingFacilityQuadTreeByType = new HashMap<>();

			for (String parkingFacilityType : this.parkingFacilitiesByType.keySet()) {

				log.info("building parking garage QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
				QuadTree<Id<ActivityFacility>> quadTree = new QuadTree<>(minx, miny, maxx, maxy);
				for (ParkingFacility f : this.parkingFacilitiesByType.get(parkingFacilityType).values()) {
					quadTree.put(f.getCoord().getX(), f.getCoord().getY(), f.getId());
				}
				/* assign the quadTree at the very end, when it is complete.
				 * otherwise, other threads may already start working on an incomplete quadtree
				 */
				this.parkingFacilityQuadTreeByType.putIfAbsent(parkingFacilityType, quadTree);
				log.info("Building parking garage QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
			}
		}
	}

	@Override
	public ZurichParkingCarVariables predict(Person person, DiscreteModeChoiceTrip trip, List<? extends PlanElement> elements) {
		if (elements.size() > 5) {
			throw new IllegalStateException("Car trips should at most contain 5 stages: 3 legs (walk, car, walk) and 2 interaction activities.");
		}

		double travelTime_min = 0.0;
		double travelCost_MU = 0.0;
		double accessEgressTime_min = 0.0;
		double euclideanDistance_km = 0.0;
		double parkingSearchTime_min = 0.0;
		double parkingCost_MU = 0.0;
		String tripPurpose = trip.getDestinationActivity().getType();

		// case of single walk leg
		if (elements.size() == 1) {
			Leg leg = (Leg) elements.get(0);
			accessEgressTime_min = leg.getTravelTime().seconds() / 60.0;
		}

		// case of car leg surrounded by 2 walk legs
		else if (elements.size() == 5) {
			Leg accessWalk = (Leg) elements.get(0);
			Leg carLeg = (Leg) elements.get(2);
			Leg egressWalk = (Leg) elements.get(4);

			// Compute trip attribute estimates
			accessEgressTime_min = (accessWalk.getTravelTime().seconds() + egressWalk.getTravelTime().seconds()) / 60.0;
			travelTime_min = carLeg.getTravelTime().seconds() / 60.0;
			travelCost_MU = costModel.calculateCost_MU(person, trip, elements);
			euclideanDistance_km = PredictorUtils.calculateEuclideanDistance_km(trip);

			// Determine the parking search strategy for this activity
			Activity destinationActivity = trip.getDestinationActivity();
			String parkingSearchStrategy = destinationActivity.getAttributes().getAttribute("parkingSearchStrategy").toString();

			// if DriveToDestination, set search strategy to DriveToDestination and parking facility id to "outside"
			if (parkingSearchStrategy.equals(ParkingSearchStrategy.DriveToDestination.toString())) {
				carLeg.getAttributes().putAttribute("parkingSearchStrategy", parkingSearchStrategy);
				carLeg.getAttributes().putAttribute("parkingFacilityId", Id.create("outside", ActivityFacility.class));
			}
			// if Random, they need to search and pay for it
			else if (parkingSearchStrategy.equals(ParkingSearchStrategy.Random.toString())) {

				// we need to choose whether to parking on street or in a parking garage
				ParkingSearchStrategy selectedSearchStrategy = null;

				// 1 - compute the route to the desired destination
				NetworkRoute networkRouteToDestination = this.router.getRouteFromParkingToDestination(
						carLeg.getRoute().getEndLinkId(),
						carLeg.getDepartureTime().seconds(),
						carLeg.getRoute().getStartLinkId());

				// 2 - cut back route to 1km radius around destination (this is our parking search start point)
				Id<Link> parkingSearchStartLinkId = networkRouteToDestination.getEndLinkId();
				{
					List<Id<Link>> networkRouteToDestinationIds = networkRouteToDestination.getLinkIds();
					ListIterator<Id<Link>> listIterator = networkRouteToDestinationIds.listIterator(networkRouteToDestinationIds.size());

					while (listIterator.hasPrevious()) {
						parkingSearchStartLinkId = listIterator.previous();
						Link parkingSearchStartLink = scenario.getNetwork().getLinks().get(parkingSearchStartLinkId);
						double distanceToDestination = CoordUtils.calcEuclideanDistance(parkingSearchStartLink.getCoord(), destinationActivity.getCoord());
						if (distanceToDestination > 1e3) {
							break;
						}
					}
				}

				// get 2 subroutes, from origin to parking search start, and from parking search start to destination
				// this defines the based travel times and distances
				NetworkRoute networkRouteFromOriginToParkingSearchStart = this.router.getRouteFromParkingToDestination(
						parkingSearchStartLinkId,
						carLeg.getDepartureTime().seconds(),
						networkRouteToDestination.getStartLinkId());

				NetworkRoute networkRouteFromParkingSearchStartToDestination = this.router.getRouteFromParkingToDestination(
						networkRouteToDestination.getEndLinkId(),
						carLeg.getDepartureTime().seconds() + networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds(),
						parkingSearchStartLinkId);

				// 3 - Find the nearest parking garage to destination
				Id<ActivityFacility> parkingGarageId = this.parkingFacilityQuadTreeByType.get(ParkingFacilityType.Garage.toString()).getClosest(destinationActivity.getCoord().getX(), destinationActivity.getCoord().getY());
				ParkingFacility parkingGarage = this.parkingFacilitiesByType.get(ParkingFacilityType.Garage.toString()).get(parkingGarageId);
				double distanceNearestGarage_m = CoordUtils.calcEuclideanDistance(parkingGarage.getCoord(), destinationActivity.getCoord());

				// 4 - Generate candidate parking options
				MultinomialLogitSelector selector = new MultinomialLogitSelector(Double.MIN_NORMAL, Double.MAX_VALUE, false);

				// get parking end time (i.e. activity end time)
				double parkingEndTime = trip.getDestinationActivity().getEndTime().orElse(30 * 3600.0);

				// always have a garage option
				{
					// get route to parking garage
					NetworkRoute networkRouteToGarage = this.router.getRouteFromParkingToDestination(parkingGarage.getLinkId(),
							carLeg.getDepartureTime().seconds() + networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds(),
							networkRouteFromOriginToParkingSearchStart.getStartLinkId());

					// check if we can park there
					double garageParkingStartTime = carLeg.getDepartureTime().seconds() +
							networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds() +
							networkRouteToGarage.getTravelTime().seconds();

					// if we can park there, generate the candidate
					if (parkingGarage.isAllowedToPark(garageParkingStartTime, parkingEndTime, person.getId(), tripPurpose)) {
						double garageCandidateTravelDistance_m = networkRouteToGarage.getDistance();
						double garageCandidateTravelTime_sec = networkRouteToGarage.getTravelTime().seconds();
						double garageCandidateEgressDistance_m = distanceNearestGarage_m * 1.4;
						double garageCandidateEgressTime_sec = garageCandidateEgressDistance_m / 1e3 * 3600.0 / 5;
						double garageCandidateParkingCost = parkingGarage.getParkingCost(garageParkingStartTime, parkingEndTime);

						selector.addCandidate(new ParkingCandidate("garage",
								garageCandidateTravelTime_sec, garageCandidateTravelDistance_m,
								0.0, 0.0,
								garageCandidateEgressTime_sec, garageCandidateEgressDistance_m,
								garageCandidateParkingCost));
					}
				}

				// generate the on-street parking candidate
				// TODO: current approach is only valid for the random walk,
				//  which assumes we first travel to our destination before starting our search

				// get search start time and coord
				double onStreetSearchStartTime = trip.getDepartureTime() + networkRouteToDestination.getTravelTime().seconds();
				Coord destinationCoord = destinationActivity.getCoord();

				// Worst case (price-wise) is that we park in a high tariff white zone
				Id<ActivityFacility> onStreetParkingId = this.parkingFacilityQuadTreeByType.get(ParkingFacilityType.HighTariffWhiteZone.toString()).getClosest(destinationActivity.getCoord().getX(), destinationActivity.getCoord().getY());
				ParkingFacility onStreetParkingFacility = this.parkingFacilitiesByType.get(ParkingFacilityType.HighTariffWhiteZone.toString()).get(onStreetParkingId);
				double distanceNearestOnStreetParkingFacility_m = CoordUtils.calcEuclideanDistance(onStreetParkingFacility.getCoord(), destinationActivity.getCoord());

				// Check if we have a closer low-tariff white zone space
				{
					String candidateParkingType = ParkingFacilityType.LowTariffWhiteZone.toString();
					Id<ActivityFacility> candidateParkingId = this.parkingFacilityQuadTreeByType.get(candidateParkingType).getClosest(destinationActivity.getCoord().getX(), destinationActivity.getCoord().getY());
					ParkingFacility candidateParkingFacility = this.parkingFacilitiesByType.get(candidateParkingType).get(candidateParkingId);
					double distanceNearestCandidateParkingFacility_m = CoordUtils.calcEuclideanDistance(candidateParkingFacility.getCoord(), destinationActivity.getCoord());

					if (distanceNearestCandidateParkingFacility_m < distanceNearestOnStreetParkingFacility_m) {
						onStreetParkingId = candidateParkingId;
						onStreetParkingFacility = candidateParkingFacility;
						distanceNearestOnStreetParkingFacility_m = distanceNearestCandidateParkingFacility_m;
					}
				}

				// Estimate if we can park in a blue zone
				{
					boolean canParkInBlueZone = new ZurichBlueZoneParking(null, null, null, 1, 3600)
							.isAllowedToPark(onStreetSearchStartTime, parkingEndTime, person.getId(), tripPurpose);

					// if we can, check if we have closer blue zone parking
					if (canParkInBlueZone) {
						String candidateParkingType = ParkingFacilityType.BlueZone.toString();
						Id<ActivityFacility> candidateParkingId = this.parkingFacilityQuadTreeByType.get(candidateParkingType).getClosest(destinationActivity.getCoord().getX(), destinationActivity.getCoord().getY());
						ParkingFacility candidateParkingFacility = this.parkingFacilitiesByType.get(candidateParkingType).get(candidateParkingId);
						double distanceNearestCandidateParkingFacility_m = CoordUtils.calcEuclideanDistance(candidateParkingFacility.getCoord(), destinationActivity.getCoord());

						if (distanceNearestCandidateParkingFacility_m < distanceNearestOnStreetParkingFacility_m) {
							onStreetParkingId = candidateParkingId;
							onStreetParkingFacility = candidateParkingFacility;
							distanceNearestOnStreetParkingFacility_m = distanceNearestCandidateParkingFacility_m;
						}
					}
				}

				// update route to destination to on-street parking candidate
				networkRouteFromParkingSearchStartToDestination = this.router.getRouteFromParkingToDestination(
						onStreetParkingFacility.getLinkId(),
						carLeg.getDepartureTime().seconds() + networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds(),
						networkRouteFromOriginToParkingSearchStart.getStartLinkId());
				onStreetSearchStartTime = trip.getDepartureTime() +
						networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds() +
						networkRouteFromParkingSearchStartToDestination.getTravelTime().seconds();
				destinationCoord = onStreetParkingFacility.getCoord();

				// get estimate from listener
				double onStreetCandidateSearchTime_sec = parkingListener.getParkingSearchTimeAtCoordAtTime(destinationCoord, onStreetSearchStartTime);
				double onStreetCandidateSearchDistance_m = parkingListener.getParkingSearchDistanceAtCoordAtTime(destinationCoord, onStreetSearchStartTime);
				double onStreetCandidateEgressDistance_m = Math.max(distanceNearestOnStreetParkingFacility_m,
						parkingListener.getEgressDistanceAtCoordAtTime(destinationCoord, onStreetSearchStartTime));

				// compute walk time
				double beelineDistanceFactor = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(TransportMode.walk).getBeelineDistanceFactor();
				double walkSpeed = scenario.getConfig().plansCalcRoute().getModeRoutingParams().get(TransportMode.walk).getTeleportedModeSpeed();
				double onStreetCandidateEgressTime_sec = onStreetCandidateEgressDistance_m * beelineDistanceFactor / walkSpeed;

				// compute parking costs
				double onStreetArrivalTime = onStreetSearchStartTime + onStreetCandidateSearchTime_sec;
				double onStreetCandidateParkingCost = onStreetParkingFacility.getParkingCost(onStreetArrivalTime, parkingEndTime);

				double onStreetCandidateTravelTime_sec = networkRouteFromParkingSearchStartToDestination.getTravelTime().seconds() + onStreetCandidateSearchTime_sec;
				double onStreetCandidateTravelDistance_m = networkRouteFromParkingSearchStartToDestination.getDistance() + onStreetCandidateSearchDistance_m;
				selector.addCandidate(new ParkingCandidate("onStreet",
						onStreetCandidateTravelTime_sec, onStreetCandidateTravelDistance_m,
						onStreetCandidateSearchTime_sec, onStreetCandidateSearchDistance_m,
						onStreetCandidateEgressTime_sec, onStreetCandidateEgressDistance_m,
						onStreetCandidateParkingCost));

				// 5 - Select parking option

				// there will always be at least the on-street option
				ParkingCandidate selectedCandidate = (ParkingCandidate) selector.select(random).get();

				// modify car variables based on selected option
				if (selectedCandidate.getName().equals("garage")) {
					selectedSearchStrategy = ParkingSearchStrategy.DriveToGarage;
					travelTime_min = (networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds() + selectedCandidate.getTravelTime()) / 60.0;
					double travelDistance_m = networkRouteFromOriginToParkingSearchStart.getDistance() + selectedCandidate.getTravelDistance();
					travelCost_MU = costParameters.carCost_CHF_km * travelDistance_m / 1e3;
					accessEgressTime_min = 2 * selectedCandidate.getEgressTime() / 60.0;
					parkingSearchTime_min = selectedCandidate.getSearchTime() / 60.0;
					parkingCost_MU = selectedCandidate.getParkingCost();
					carLeg.getAttributes().putAttribute("parkingSearchStrategy", selectedSearchStrategy.toString());
					carLeg.getAttributes().putAttribute("parkingFacilityId", parkingGarage.getId());
					carLeg.getAttributes().putAttribute("parkingFacilityLinkId", parkingGarage.getLinkId());
				} else {
					selectedSearchStrategy = ParkingSearchStrategy.Random;
					travelTime_min = (networkRouteFromOriginToParkingSearchStart.getTravelTime().seconds() + selectedCandidate.getTravelTime() - selectedCandidate.getSearchTime()) / 60.0;
					double travelDistance_m = networkRouteFromOriginToParkingSearchStart.getDistance() + selectedCandidate.getTravelDistance();
					travelCost_MU = costParameters.carCost_CHF_km * travelDistance_m / 1e3;
					accessEgressTime_min = 2 * selectedCandidate.getEgressTime() / 60.0;
					parkingSearchTime_min = selectedCandidate.getSearchTime() / 60.0;
					parkingCost_MU = selectedCandidate.getParkingCost();
					carLeg.getAttributes().putAttribute("parkingSearchStrategy", selectedSearchStrategy.toString());
					carLeg.getAttributes().putAttribute("parkingFacilityId", onStreetParkingFacility.getId());
					carLeg.getAttributes().putAttribute("parkingFacilityLinkId", onStreetParkingFacility.getLinkId());
				}
			}
			// else, throw error
			else {
				throw new IllegalStateException("Unexpected parkingSearchStrategy value: " + parkingSearchStrategy);
			}
		}

		else {
			throw new IllegalStateException("Car trip contains " + elements.size() + " legs! Not sure what to do here.");
		}

		return new ZurichParkingCarVariables(travelTime_min, parkingSearchTime_min,  travelCost_MU, parkingCost_MU,
				euclideanDistance_km, accessEgressTime_min);

	}

	private class ParkingCandidate implements UtilityCandidate {
		private String name;
		private double travelTime;
		private double travelDistance;
		private double searchTime;
		private double searchDistance;
		private double egressTime;
		private double egressDistance;
		private double parkingCost;
		private ParkingChoiceModelParameters parameters = new ParkingChoiceModelParameters();

		public ParkingCandidate(String name, double travelTime, double travelDistance,
								double searchTime, double searchDistance,
								double egressTime, double egressDistance,
								double cost) {
			this.name = name;
			this.travelTime = travelTime;
			this.travelDistance = travelDistance;
			this.searchTime = searchTime;
			this.searchDistance = searchDistance;
			this.egressTime = egressTime;
			this.egressDistance = egressDistance;
			this.parkingCost = cost;
		}

		public String getName() {
			return name;
		}

		public double getTravelTime() {
			return travelTime;
		}

		public double getTravelDistance() { return travelDistance; }

		public double getSearchTime() { return searchTime; }

		public double getSearchDistance() { return searchDistance; }

		public double getEgressTime() {
			return egressTime;
		}

		public double getEgressDistance() { return egressDistance; }

		public double getParkingCost() {
			return parkingCost;
		}

		public ParkingChoiceModelParameters getParameters() {
			return parameters;
		}

		@Override
		public double getUtility() {
			double utility = parameters.alpha;
			utility += parameters.beta_travelDistance * this.travelDistance / 1e3;
			utility += parameters.beta_egressDistance * this.egressDistance / 1e3;
			utility += parameters.beta_parkingCost * this.parkingCost;
			return utility;
		}
	}

	private class ParkingChoiceModelParameters {
		public double alpha = -1.778;
		public double beta_travelDistance = -0.228;
		//		public double beta_travelTime = beta_travelDistance * 15 / 3600.0;
		public double beta_egressDistance = -3.390;
		//		public double beta_egressTime = beta_egressDistance * 5 / 3600.0;
		public double beta_parkingCost = -0.101;
	}

}
