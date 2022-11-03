package org.eqasim.examples.zurich_parking.analysis.parking;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.events.ParkingEvent;
import org.matsim.contrib.parking.parkingsearch.events.ParkingEventHandler;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEvent;
import org.matsim.contrib.parking.parkingsearch.events.StartParkingSearchEventHandler;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.OptionalTime;

import java.io.IOException;
import java.util.*;

public class ParkingSearchMetricsListener implements
		//PersonDepartureEventHandler,
		StartParkingSearchEventHandler, LinkEnterEventHandler, ParkingEventHandler, ActivityStartEventHandler,
		IterationEndsListener {

	private final Map<Id<Person>, ParkingSearchItem> ongoing = new HashMap<>();
	private final Map<Id<Person>, Integer> tripIds = new HashMap<>();
	private final List<ParkingSearchItem> trips = new LinkedList<>();
	private final Network network;

	@Inject
	public ParkingSearchMetricsListener(Network network) {
		this.network = network;
	}

	@Override
	public void handleEvent(StartParkingSearchEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		if (!ongoing.containsKey(personId)) {
			// increment trip id
			tripIds.putIfAbsent(personId, -1);
			int tripId = tripIds.get(personId) + 1;
			tripIds.put(personId, tripId);
			ParkingSearchItem trip = new ParkingSearchItem();

			trip.personId = personId;
			trip.tripId = tripIds.get(personId);
			ongoing.put(personId, trip);
		}

		ParkingSearchItem trip = ongoing.get(personId);
		trip.parkingSearchStartTime = OptionalTime.defined(event.getTime());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		// vehicle and person ids should be the same
		Id<Person> personId = Id.createPersonId(event.getVehicleId());
		if (ongoing.containsKey(personId)) {
			ParkingSearchItem trip = ongoing.get(personId);
			if (trip.parkingSearchStartTime.isDefined() && trip.parkingSearchEndTime.isUndefined()) {
				Id<Link> linkId = event.getLinkId();
				double linkLength = this.network.getLinks().get(linkId).getLength();
				trip.parkingSearchDistance += linkLength;
				if (trip.searchedLinkIds.contains(linkId)) {
					trip.duplicatedDistance += linkLength;
				}
				trip.searchedLinkIds.add(linkId);
			}
		}
	}

	@Override
	public void handleEvent(ParkingEvent event) {
		Id<Person> personId = Id.createPersonId(event.getVehicleId());

		// if there was no search prior, then there will not yet be an ongoing trip registered
		if (!ongoing.containsKey(personId)) {
			// increment trip id
			tripIds.putIfAbsent(personId, -1);
			int tripId = tripIds.get(personId) + 1;
			tripIds.put(personId, tripId);
			ParkingSearchItem trip = new ParkingSearchItem();

			trip.personId = personId;
			trip.tripId = tripIds.get(personId);
			trip.parkingSearchStartTime = OptionalTime.defined(event.getTime());
			ongoing.put(personId, trip);
		}

		ParkingSearchItem trip = ongoing.get(personId);
		trip.parkingSearchEndTime = OptionalTime.defined(event.getTime());
		trip.parkingSearchTime = trip.parkingSearchEndTime.seconds() - trip.parkingSearchStartTime.seconds();

		// get parking facility attributes
		trip.parkingFacilityId = Optional.ofNullable(event.getParkingFacilityId());
		trip.parkingFacilityType = Optional.ofNullable(event.getParkingFacilityType());
		trip.parkingCoord = Optional.ofNullable(event.getParkingFacilityCoord());
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		Id<Person> personId = event.getPersonId();

		// check if we have this trip ongoing
		if (ongoing.containsKey(personId)) {

			// check if this is not a car interaction activity
			if (!event.getActType().contains(ParkingUtils.PARKACTIVITYTYPE)) {

				ParkingSearchItem trip = ongoing.remove(personId);

				// if we have parked our vehicle already
				if (trip.parkingCoord.isPresent()) {
					trip.arrivalTime = OptionalTime.defined(event.getTime());
					trip.destinationCoord = network.getLinks().get(event.getLinkId()).getCoord();
					trip.egressWalkDistance = CoordUtils.calcEuclideanDistance(trip.parkingCoord.get(), trip.destinationCoord);
					if (trip.parkingSearchEndTime.isDefined()) {
						trip.egressWalkTime = trip.arrivalTime.seconds() - trip.parkingSearchEndTime.seconds();
					}
					trip.tripPurpose = event.getActType();

					// add to list of trips
					trips.add(trip);
				}
			}
		}
	}

	public List<ParkingSearchItem> getTrips() {
		return trips;
	}

	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		ParkingSearchMetricsWriter writer = new ParkingSearchMetricsWriter(this.getTrips());
		try {
			writer.write(event.getServices().getControlerIO().getIterationPath(event.getIteration()) +
					"/" + event.getIteration() + ".parking_search_metrics.csv");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		reset(event.getIteration());
	}

	@Override
	public void reset(int iteration) {
		// clear all maps
		ongoing.clear();
		trips.clear();
	}

}

