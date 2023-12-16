package org.eqasim.core.simulation.modes.drt.analysis.passengers;

import org.eqasim.core.simulation.modes.drt.analysis.utils.LinkFinder;
import org.eqasim.core.simulation.modes.drt.analysis.utils.PassengerTracker;
import org.eqasim.core.simulation.modes.drt.analysis.utils.VehicleRegistry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;

import java.util.*;

public class PassengerAnalysisListener implements PersonDepartureEventHandler, PersonArrivalEventHandler,
		LinkEnterEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {
	private final LinkFinder linkFinder;
	private final VehicleRegistry vehicleRegistry;
	private final Collection<String> modes;
	private final PassengerTracker passengers = new PassengerTracker();

	private final List<PassengerRideItem> rides = new LinkedList<>();
	private final Map<Id<Person>, PassengerRideItem> currentRides = new HashMap<>();

	public PassengerAnalysisListener(Collection<String> modes, LinkFinder linkFinder, VehicleRegistry vehicleRegistry) {
		this.linkFinder = linkFinder;
		this.modes = modes;
		this.vehicleRegistry = vehicleRegistry;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {
		if (!vehicleRegistry.isFleet(event.getPersonId())) {
			if (modes.contains(event.getLegMode())) {
				PassengerRideItem ride = new PassengerRideItem();
				rides.add(ride);

				ride.personId = event.getPersonId();
				ride.mode = event.getLegMode();

				ride.departureTime = event.getTime();
				ride.originLink = linkFinder.getLink(event.getLinkId());

				currentRides.put(event.getPersonId(), ride);
			}
		}
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		if (vehicleRegistry.isFleet(event.getVehicleId())) {
			double distance = linkFinder.getDistance(event.getLinkId());

			for (Id<Person> passengerId : passengers.getPassengerIds(event.getVehicleId())) {
				PassengerRideItem ride = currentRides.get(passengerId);

				if (ride == null) {
					throw new IllegalStateException("Found vehicle enter link without departure");
				}

				ride.distance += distance;
			}
		}
	}

	@Override
	public void handleEvent(PersonEntersVehicleEvent event) {
		if (!vehicleRegistry.isFleet(event.getPersonId())) {
			if (vehicleRegistry.isFleet(event.getVehicleId())) {
				PassengerRideItem ride = currentRides.get(event.getPersonId());

				if (ride == null) {
					throw new IllegalStateException("Found vehicle enter event without departure");
				}

				ride.vehicleId = event.getVehicleId();
				ride.waitingTime = event.getTime() - ride.departureTime;

				passengers.addPassenger(event.getVehicleId(), event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonLeavesVehicleEvent event) {
		if (!vehicleRegistry.isFleet(event.getPersonId())) {
			if (vehicleRegistry.isFleet(event.getVehicleId())) {
				passengers.removePassenger(event.getVehicleId(), event.getPersonId());
			}
		}
	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {
		if (!vehicleRegistry.isFleet(event.getPersonId())) {
			PassengerRideItem ride = currentRides.remove(event.getPersonId());

			if (ride != null) {
				ride.arrivalTime = event.getTime();
				ride.destinationLink = linkFinder.getLink(event.getLinkId());
			}
		}
	}

	@Override
	public void reset(int iteration) {
		passengers.clear();
		rides.clear();
		currentRides.clear();
	}

	public List<PassengerRideItem> getRides() {
		return rides;
	}
}
