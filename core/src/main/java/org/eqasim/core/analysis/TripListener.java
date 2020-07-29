package org.eqasim.core.analysis;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class TripListener implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler,
		TeleportationArrivalEventHandler {
	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	final private Network network;
	final private PopulationFactory factory;
	final private Collection<String> networkRouteModes;

	final private Collection<TripItem> trips = new LinkedList<>();
	final private Collection<LegItem> legs = new LinkedList<>();

	final private Map<Id<Person>, TripListenerItem> ongoingTrip = new HashMap<>();
	final private Map<Id<Person>, LegListenerItem> ongoingLeg = new HashMap<>();
	final private Map<Id<Vehicle>, Collection<Id<Person>>> passengers = new HashMap<>();

	final private Map<Id<Person>, Integer> tripIndex = new HashMap<>();
	final private Map<Id<Person>, Integer> legIndex = new HashMap<>();

	final private PersonAnalysisFilter personFilter;

	public TripListener(Network network, StageActivityTypes stageActivityTypes, MainModeIdentifier mainModeIdentifier,
						Collection<String> networkRouteModes, PersonAnalysisFilter personFilter) {
		this.network = network;
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.networkRouteModes = networkRouteModes;
		this.factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		this.personFilter = personFilter;
	}

	public Collection<TripItem> getTripItems() {
		return trips;
	}

	public Collection<LegItem> getLegItems() {
		return legs;
	}

	@Override
	public void reset(int iteration) {
		trips.clear();
		legs.clear();
		ongoingTrip.clear();
		ongoingLeg.clear();
		passengers.clear();
		tripIndex.clear();
		legIndex.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {

			// update ongoing trips
			Integer personTripIndex = tripIndex.get(event.getPersonId());

			if (!stageActivityTypes.isStageActivity(event.getActType())) {
				// this is a new trip

				if (personTripIndex == null) {
					personTripIndex = 0;
				} else {
					personTripIndex = personTripIndex + 1;
				}

				ongoingTrip.put(event.getPersonId(), new TripListenerItem(event.getPersonId(), personTripIndex,
						network.getLinks().get(event.getLinkId()).getCoord(), event.getTime(), event.getActType()));

				tripIndex.put(event.getPersonId(), personTripIndex);
			}

			// update ongoing legs
			Integer personLegIndex = legIndex.get(event.getPersonId());

			if (personLegIndex == null) {
				personLegIndex = 0;
			} else {
				personLegIndex = personLegIndex + 1;
			}

			ongoingLeg.put(event.getPersonId(), new LegListenerItem(event.getPersonId(), personTripIndex, personLegIndex,
					network.getLinks().get(event.getLinkId()).getCoord(), event.getTime(), event.getActType()));

			legIndex.put(event.getPersonId(), personLegIndex);
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			ongoingTrip.get(event.getPersonId()).elements.add(factory.createLeg(event.getLegMode()));
			ongoingLeg.get(event.getPersonId()).mode = event.getLegMode();
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {

			if (stageActivityTypes.isStageActivity(event.getActType())) {
				ongoingTrip.get(event.getPersonId()).elements
						.add(factory.createActivityFromLinkId(event.getActType(), event.getLinkId()));
			} else {

				// process the trip
				TripListenerItem trip = ongoingTrip.remove(event.getPersonId());

				if (trip != null) {
					trip.returning = event.getActType().equals("home");
					trip.followingPurpose = event.getActType();
					trip.travelTime = event.getTime() - trip.startTime;
					trip.mode = mainModeIdentifier.identifyMainMode(trip.elements);
					trip.destination = network.getLinks().get(event.getLinkId()).getCoord();
					trip.networkDistance = getNetworkDistance(trip);
					trip.crowflyDistance = CoordUtils.calcEuclideanDistance(trip.origin, trip.destination);

					trips.add(new TripItem(trip.personId, trip.tripId, trip.origin, trip.destination,
							trip.startTime, trip.travelTime, trip.networkDistance, trip.mode, trip.preceedingPurpose,
							trip.followingPurpose, trip.returning, trip.crowflyDistance));

				}

				// reset the leg index
				legIndex.remove(event.getPersonId());
			}

			// process the legs
			LegListenerItem leg = ongoingLeg.remove(event.getPersonId());

			if (leg != null) {
				leg.returning = event.getActType().equals("home");
				leg.followingPurpose = event.getActType();
				leg.travelTime = event.getTime() - leg.startTime;
				leg.destination = network.getLinks().get(event.getLinkId()).getCoord();
				leg.networkDistance = getNetworkDistance(leg);
				leg.crowflyDistance = CoordUtils.calcEuclideanDistance(leg.origin, leg.destination);

				legs.add(new LegItem(leg.personId, leg.tripId, leg.legId, leg.origin, leg.destination,
						leg.startTime, leg.travelTime, leg.networkDistance, leg.mode, leg.preceedingPurpose,
						leg.followingPurpose, leg.returning, leg.crowflyDistance));

			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!passengers.containsKey(event.getVehicleId())) {
				passengers.put(event.getVehicleId(), new HashSet<>());
			}

			passengers.get(event.getVehicleId()).add(event.getPersonId());
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (passengers.containsKey(event.getVehicleId())) {
				passengers.get(event.getVehicleId()).remove(event.getPersonId());

				if (passengers.get(event.getVehicleId()).size() == 0) {
					passengers.remove(event.getVehicleId());
				}
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Collection<Id<Person>> personIds = passengers.get(event.getVehicleId());

		if (personIds != null) {
			personIds.forEach(id -> ongoingTrip.get(id).route.add(event.getLinkId()));
			personIds.forEach(id -> ongoingLeg.get(id).route.add(event.getLinkId()));
		}
	}

	private double getNetworkDistance(TripListenerItem trip) {
		if (networkRouteModes.contains(mainModeIdentifier.identifyMainMode(trip.elements))) {
			double distance = 0.0;

			if (trip.route.size() > 0) {
				for (Id<Link> linkId : trip.route.subList(0, trip.route.size() - 1)) {
					distance += network.getLinks().get(linkId).getLength();
				}
			}

			return distance;
		}

		return trip.networkDistance;
	}

	private double getNetworkDistance(LegListenerItem leg) {
		if (networkRouteModes.contains(leg.mode)) {
			double distance = 0.0;

			if (leg.route.size() > 0) {
				for (Id<Link> linkId : leg.route.subList(0, leg.route.size() - 1)) {
					distance += network.getLinks().get(linkId).getLength();
				}
			}

			return distance;
		}

		return leg.networkDistance;
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {

			// update trip distance
			TripListenerItem trip = ongoingTrip.get(event.getPersonId());

			if (Double.isNaN(trip.networkDistance)) {
				trip.networkDistance = 0.0;
			}

			trip.networkDistance += event.getDistance();

			// update leg distance
			LegListenerItem leg = ongoingLeg.get(event.getPersonId());

			if (Double.isNaN(leg.networkDistance)) {
				leg.networkDistance = 0.0;
			}

			leg.networkDistance += event.getDistance();
		}
	}
}