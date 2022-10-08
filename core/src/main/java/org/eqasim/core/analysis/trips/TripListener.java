package org.eqasim.core.analysis.trips;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.eqasim.core.analysis.PersonAnalysisFilter;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.GenericEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.GenericEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.vehicles.Vehicle;

public class TripListener implements ActivityStartEventHandler, ActivityEndEventHandler, PersonDepartureEventHandler,
		PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler,
		TeleportationArrivalEventHandler, GenericEventHandler {
	final private MainModeIdentifier mainModeIdentifier;
	final private Network network;
	final private PopulationFactory factory;

	final private Collection<TripItem> trips = new LinkedList<>();
	final private Map<Id<Person>, TripListenerItem> ongoing = new HashMap<>();
	final private Map<Id<Vehicle>, Collection<Id<Person>>> passengers = new HashMap<>();
	final private Map<Id<Person>, Integer> tripIndex = new HashMap<>();

	final private PersonAnalysisFilter personFilter;

	final private ScenarioExtent scenarioExtent;

	final private TransitSchedule transitSchedule;


	public TripListener(Network network, MainModeIdentifier mainModeIdentifier, PersonAnalysisFilter personFilter) {
		this(network, mainModeIdentifier, personFilter, null, null);
	}

	public TripListener(Network network, MainModeIdentifier mainModeIdentifier, PersonAnalysisFilter personFilter, ScenarioExtent scenarioExtent, TransitSchedule transitSchedule) {
		this.network = network;
		this.mainModeIdentifier = mainModeIdentifier;
		this.factory = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation().getFactory();
		this.personFilter = personFilter;
		this.scenarioExtent = scenarioExtent;
		this.transitSchedule = transitSchedule;
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
			if (!TripStructureUtils.isStageActivityType(event.getActType())) {
				Integer personTripIndex = tripIndex.get(event.getPersonId());
				network.getLinks().get(event.getLinkId()).getCoord();

				if (personTripIndex == null) {
					personTripIndex = 0;
				} else {
					personTripIndex = personTripIndex + 1;
				}

				TripListenerItem tripListenerItem = new TripListenerItem(event.getPersonId(), personTripIndex,
						network.getLinks().get(event.getLinkId()).getCoord(), event.getTime(), event.getActType());
				tripListenerItem.originScope = this.scenarioExtent == null ? "inside" : this.scenarioExtent.isInside(tripListenerItem.origin) ? "inside": "outside";
				if(tripListenerItem.originScope.equals("inside")) {
					tripListenerItem.tripScope = "inside";
				}
				ongoing.put(event.getPersonId(), tripListenerItem);
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
			if (TripStructureUtils.isStageActivityType(event.getActType())) {
				ongoing.get(event.getPersonId()).elements
						.add(factory.createActivityFromLinkId(event.getActType(), event.getLinkId()));
			} else {
				TripListenerItem trip = ongoing.remove(event.getPersonId());

				if (trip != null) {
					trip.returning = event.getActType().equals("home");
					trip.followingPurpose = event.getActType();
					trip.travelTime = event.getTime() - trip.departureTime;
					trip.mode = mainModeIdentifier.identifyMainMode(trip.elements);
					trip.destination = network.getLinks().get(event.getLinkId()).getCoord();
					trip.destinationScope = this.scenarioExtent == null ? "" : this.scenarioExtent.isInside(trip.destination) ? "inside" : "outside";
					if(trip.destinationScope.equals("inside")) {
						trip.tripScope = "inside";
					}
					if(trip.tripScope.equals("") && this.scenarioExtent != null) {
						trip.tripScope = "outside";
					}
					trip.euclideanDistance = CoordUtils.calcEuclideanDistance(trip.origin, trip.destination);

					trips.add(new TripItem(trip.personId, trip.personTripId, trip.origin, trip.destination,
							trip.departureTime, trip.travelTime, trip.vehicleDistance, trip.routedDistance, trip.mode,
							trip.precedingPurpose, trip.followingPurpose, trip.returning, trip.euclideanDistance, trip.originScope, trip.destinationScope, trip.tripScope));
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

				// Last link is not traversed, so we should not count it!
				TripListenerItem item = ongoing.get(event.getPersonId());
				item.routedDistance -= item.lastAddedLinkDistance;
				item.vehicleDistance -= item.lastAddedLinkDistance;
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		Collection<Id<Person>> personIds = passengers.get(event.getVehicleId());

		if (personIds != null) {
			personIds.forEach(id -> {
				Link link = network.getLinks().get(event.getLinkId());
				double linkDistance = link.getLength();
				TripListenerItem item = ongoing.get(id);
				if(this.scenarioExtent != null && this.scenarioExtent.isInside(link.getCoord())) {
					item.tripScope = "inside";
				}
				item.routedDistance += linkDistance;
				item.vehicleDistance += linkDistance;
				item.lastAddedLinkDistance = linkDistance;
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

	@Override
	public void handleEvent(GenericEvent event) {
		if (event instanceof PublicTransitEvent) {
			PublicTransitEvent transitEvent = (PublicTransitEvent) event;

			if (personFilter.analyzePerson(transitEvent.getPersonId())) {
				TripListenerItem item = ongoing.get(transitEvent.getPersonId());
				item.vehicleDistance += transitEvent.getTravelDistance();
				if(this.scenarioExtent != null) {
					TransitRoute transitRoute = this.transitSchedule.getTransitLines().get(transitEvent.getTransitLineId()).getRoutes().get(transitEvent.getTransitRouteId());
					for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
						if(this.scenarioExtent.isInside(transitRouteStop.getStopFacility().getCoord())){
							item.tripScope = "inside";
							break;
						}
					}
					if(item.tripScope.equals("inside")) {
						return;
					}
					for(Id<Link> linkId: transitRoute.getRoute().getLinkIds()) {
						if(this.scenarioExtent.isInside(network.getLinks().get(linkId).getCoord())) {
							item.tripScope = "inside";
							break;
						}
					}
				}
			}
		}
	}
}