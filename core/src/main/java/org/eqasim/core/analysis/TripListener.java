package org.eqasim.core.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
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

public class TripListener implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler,
		TeleportationArrivalEventHandler {
	final private StageActivityTypes stageActivityTypes;
	final private MainModeIdentifier mainModeIdentifier;
	final private Network network;
	final private PopulationFactory factory;

	final private Collection<TripItem> trips = new LinkedList<>();
	final private Map<Id<Person>, TripListenerItem> ongoing = new HashMap<>();
	final private Map<Id<Vehicle>, Collection<Id<Person>>> passengers = new HashMap<>();
	final private Map<Id<Person>, Integer> tripIndex = new HashMap<>();

	final private PersonAnalysisFilter personFilter;

	public TripListener(Network network, StageActivityTypes stageActivityTypes, MainModeIdentifier mainModeIdentifier,
			PersonAnalysisFilter personFilter) {
		this.network = network;
		this.stageActivityTypes = stageActivityTypes;
		this.mainModeIdentifier = mainModeIdentifier;
		this.factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		this.personFilter = personFilter;
	}

	public Collection<TripItem> getTripItems() {
		return trips;
	}

	@Override
	public void reset(int iteration) {
		trips.clear();
		ongoing.clear();
		passengers.clear();
		tripIndex.clear();
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (!stageActivityTypes.isStageActivity(event.getActType())) {
				Integer personTripIndex = tripIndex.get(event.getPersonId());
				network.getLinks().get(event.getLinkId()).getCoord();

				if (personTripIndex == null) {
					personTripIndex = 0;
				} else {
					personTripIndex = personTripIndex + 1;
				}

				ongoing.put(event.getPersonId(), new TripListenerItem(event.getPersonId(), personTripIndex,
						network.getLinks().get(event.getLinkId()).getCoord(), event.getTime(), event.getActType()));

				tripIndex.put(event.getPersonId(), personTripIndex);
			}
		}
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			ongoing.get(event.getPersonId()).elements.add(factory.createLeg(event.getLegMode()));
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			if (stageActivityTypes.isStageActivity(event.getActType())) {
				ongoing.get(event.getPersonId()).elements
						.add(factory.createActivityFromLinkId(event.getActType(), event.getLinkId()));
			} else {
				TripListenerItem trip = ongoing.remove(event.getPersonId());

				if (trip != null) {
					trip.returning = event.getActType().equals("home");
					trip.followingPurpose = event.getActType();
					trip.travelTime = event.getTime() - trip.startTime;
					trip.mode = mainModeIdentifier.identifyMainMode(trip.elements);
					trip.destination = network.getLinks().get(event.getLinkId()).getCoord();
					trip.crowflyDistance = CoordUtils.calcEuclideanDistance(trip.origin, trip.destination);

					trips.add(new TripItem(trip.personId, trip.personTripId, trip.origin, trip.destination,
							trip.startTime, trip.travelTime, trip.networkDistance, trip.routedDistance, trip.mode,
							trip.preceedingPurpose, trip.followingPurpose, trip.returning, trip.crowflyDistance));
				}
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
			personIds.forEach(id -> {
				ongoing.get(id).routedDistance += network.getLinks().get(event.getLinkId()).getLength();
				ongoing.get(id).networkDistance += network.getLinks().get(event.getLinkId()).getLength();
			});
		}
	}

	@Override
	public void handleEvent(TeleportationArrivalEvent event) {
		if (personFilter.analyzePerson(event.getPersonId())) {
			TripListenerItem item = ongoing.get(event.getPersonId());
			item.routedDistance += event.getDistance();
		}
	}
}