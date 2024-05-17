/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.eqasim.core.analysis.cba.analyzers.drtAnalysis;

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.PassengerPickedUpEvent;

import java.util.function.Function;

final class DrtTrip {
	final Id<Request> request;
	final double departureTime;
	final Id<Person> person;
	final Id<DvrpVehicle> vehicle;
	final Id<Link> fromLinkId;
	final Coord fromCoord;
	final Id<Link> toLink;
	final Coord toCoord;
	final double waitTime;
	final double unsharedDistanceEstimate_m;
	final double unsharedTimeEstimate_m;
	final double arrivalTime;
	final double fare;
	final double traveledDistance;
	final String beforeActivityType;
	final String afterActivityType;
	final double accessTime;
	final double egressTime;

	DrtTrip(DrtAnalyzer.PerformedRequestEventSequence sequence, Function<Id<Link>, ? extends Link> linkProvider, DrtConfigGroup.OperationalScheme scheme) {
		Preconditions.checkArgument(sequence.isCompleted());
		DrtRequestSubmittedEvent submittedEvent = sequence.getSubmitted();
		PassengerPickedUpEvent pickedUpEvent = sequence.getPickedUp().get();
		this.request = submittedEvent.getRequestId();
		this.departureTime = submittedEvent.getTime();
		this.person = submittedEvent.getPersonId();
		this.vehicle = pickedUpEvent.getVehicleId();
		this.fromLinkId = submittedEvent.getFromLinkId();
		this.fromCoord = linkProvider.apply(fromLinkId).getCoord();
		this.toLink = submittedEvent.getToLinkId();
		this.toCoord = linkProvider.apply(toLink).getCoord();
		this.waitTime = pickedUpEvent.getTime() - submittedEvent.getTime();
		this.unsharedDistanceEstimate_m = submittedEvent.getUnsharedRideDistance();
		this.unsharedTimeEstimate_m = submittedEvent.getUnsharedRideTime();
		this.arrivalTime = sequence.getDroppedOff().isPresent() ? sequence.getDroppedOff().get().getTime() : -1;
		this.fare = sequence.getFare().isPresent() ? sequence.getFare().get().getAmount() : 0;
		this.traveledDistance = sequence.getDistance();
		this.beforeActivityType = sequence.getBeforeActivity().getActType();
		this.afterActivityType = sequence.getAfterActivity().getActType();
		if (scheme.equals(DrtConfigGroup.OperationalScheme.door2door)) {
			this.accessTime = 0;
			this.egressTime = 0;
		}
		else {
			this.accessTime = submittedEvent.getTime() - sequence.getBeforeActivity().getTime();
			this.egressTime = sequence.getAfterActivity().getTime() - sequence.getDroppedOff().get().getTime();
		}
	}
}
