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

/**
 *
 */
package org.eqasim.core.analysis.cba.analyzers.drtAnalysis;


import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEvent;
import org.matsim.contrib.drt.passenger.events.DrtRequestSubmittedEventHandler;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.fleet.DvrpVehicle;
import org.matsim.contrib.dvrp.fleet.Fleet;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.vehicles.Vehicle;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * TODO
 * 	Add trip motive
 * 	Time to reach the final destination (post-acheminement & pre-acheminement)
 */

public class DrtAnalyzer implements PassengerRequestRejectedEventHandler, PassengerRequestScheduledEventHandler,
        DrtRequestSubmittedEventHandler, PassengerPickedUpEventHandler, PassengerDroppedOffEventHandler, PersonMoneyEventHandler, LinkLeaveEventHandler,
        ActivityStartEventHandler, ActivityEndEventHandler, PersonScoreEventHandler, MobsimScopeEventHandler, CbaAnalyzer {

    private static final String[] TRIPS_HEADERS = new String[]{"departureTime", "personId", "vehicleId", "fromLinkId", "fromX", "fromY", "tiLinkId", "toX", "toY", "waitTime", "arrivalTime", "travelTime", "unsharedDistance", "traveledDistance" ,"fare", "motive", "accessTime", "egressTime"};
    private static final String[] VEHICLES_HEADERS = new String[]{"vehicleId", "distance"};

    private final String mode;
    private final Network network;
    private final Fleet fleet;
    private final Map<Id<Vehicle>, Double> vehiclesDistances = new HashMap<>();
    private final Map<Id<Request>, DrtRequestSubmittedEvent> requestSubmissions = new HashMap<>();
    private final Map<Id<Request>, RejectedRequestEventSequence> rejectedRequestSequences = new HashMap<>();
    private final Map<Id<Request>, PerformedRequestEventSequence> performedRequestSequences = new HashMap<>();
    private final Map<Id<Person>, Id<Request>> personsRequests = new HashMap<>();
    private final Map<Id<Person>, ActivityEndEvent> personsLastFinishedActivity = new HashMap<>();
    private final DrtConfigGroup.OperationalScheme scheme;
    private final DrtAnalyzerConfigGroup configGroup;
    private final String[] sheetsNames;

    public DrtAnalyzer(String mode, Network network, DrtConfigGroup.OperationalScheme scheme, Fleet fleet, DrtAnalyzerConfigGroup configGroup) {
        this.mode = mode;
        this.network = network;
        this.scheme = scheme;
        this.fleet = fleet;
        this.configGroup = configGroup;
        this.sheetsNames = new String[]{configGroup.getTripsSheetName(), configGroup.getVehiclesSheetName()};
    }

    private boolean isVehicleInFleet(Id<Vehicle> vehicleId) {
        Id<DvrpVehicle> id = Id.create(vehicleId, DvrpVehicle.class);
        return this.fleet.getVehicles().containsKey(id);
    }

    @Override
    public void handleEvent(LinkLeaveEvent linkLeaveEvent) {
        if(!this.isVehicleInFleet(linkLeaveEvent.getVehicleId())) {
            return;
        }
        double distance = this.network.getLinks().get(linkLeaveEvent.getLinkId()).getLength();
        this.vehiclesDistances.put(linkLeaveEvent.getVehicleId(), distance + this.vehiclesDistances.getOrDefault(linkLeaveEvent.getVehicleId(), 0.0));
    }

    @Override
    public void handleEvent(ActivityStartEvent activityStartEvent) {
        if (activityStartEvent.getActType().equals(this.mode + " interaction")) {
            return;
        }
        if (this.personsRequests.containsKey(activityStartEvent.getPersonId())) {
            PerformedRequestEventSequence sequence = this.performedRequestSequences.get(this.personsRequests.get(activityStartEvent.getPersonId()));
            if (sequence == null) {
                return;
            }
            if (sequence.afterActivity == null) {
                sequence.afterActivity = activityStartEvent;
            }
        }
    }

    @Override
    public void handleEvent(ActivityEndEvent activityEndEvent) {
        if (activityEndEvent.getActType().equals(this.mode + " interaction")) {
            return;
        }
        this.personsLastFinishedActivity.put(activityEndEvent.getPersonId(), activityEndEvent);
    }

    public DrtConfigGroup.OperationalScheme getScheme() {
        return scheme;
    }

    public Map<Id<Request>, DrtRequestSubmittedEvent> getRequestSubmissions() {
        return requestSubmissions;
    }

    public Map<Id<Request>, RejectedRequestEventSequence> getRejectedRequestSequences() {
        return rejectedRequestSequences;
    }

    public Map<Id<Request>, PerformedRequestEventSequence> getPerformedRequestSequences() {
        return performedRequestSequences;
    }


    @Override
    public void handleEvent(DrtRequestSubmittedEvent event) {
        if (event.getMode().equals(mode)) {
            requestSubmissions.put(event.getRequestId(), event);
        }
    }

    @Override
    public void handleEvent(PassengerRequestScheduledEvent event) {
        if (event.getMode().equals(mode)) {
            this.personsRequests.put(event.getPersonId(), event.getRequestId());
            performedRequestSequences.put(event.getRequestId(),
                    new PerformedRequestEventSequence(requestSubmissions.get(event.getRequestId()), event, this.personsLastFinishedActivity.get(event.getPersonId())));
        }
    }

    @Override
    public void handleEvent(PassengerRequestRejectedEvent event) {
        if (event.getMode().equals(mode)) {
            rejectedRequestSequences.put(event.getRequestId(),
                    new RejectedRequestEventSequence(requestSubmissions.get(event.getRequestId()), event));
        }
    }

    @Override
    public void handleEvent(PassengerPickedUpEvent event) {
        if (event.getMode().equals(mode)) {
            PerformedRequestEventSequence sequence = this.performedRequestSequences.get(event.getRequestId());
            sequence.pickedUp = event;
            Id<DvrpVehicle> id = Id.create(event.getVehicleId(), DvrpVehicle.class);
            sequence.distance = this.vehiclesDistances.getOrDefault(id, 0.0);
        }
    }

    @Override
    public void handleEvent(PassengerDroppedOffEvent event) {
        if (event.getMode().equals(mode)) {
            PerformedRequestEventSequence sequence = this.performedRequestSequences.get(event.getRequestId());
            sequence.droppedOff = event;
            Id<DvrpVehicle> id = Id.create(event.getVehicleId(), DvrpVehicle.class);
            sequence.distance = this.vehiclesDistances.get(id) - sequence.getDistance();
        }
    }

    @Override
    public void handleEvent(PersonMoneyEvent event) {
        if (event.getPurpose().equals("drtFare")) {
            for (PerformedRequestEventSequence sequence : this.performedRequestSequences.values()) {
                if (sequence.droppedOff != null && sequence.droppedOff.getPersonId().equals(event.getPersonId()) && sequence.droppedOff.getTime() == event.getTime()) {
                    sequence.fare = event;
                }
            }
        }
    }

    @Override
    public void handleEvent(PersonScoreEvent personScoreEvent) {
    }

    @Override
    public String[] getSheetsNames() {
        return this.sheetsNames;
    }

    @Override
    public void fillSheets(List<Sheet> sheets) {
        assert sheets.size() >= this.sheetsNames.length;
        this.fillTrips(sheets.get(0));
        this.fillVehicles(sheets.get(1));
    }

    public void fillTrips(Sheet sheet) {
        assert sheet.getSheetName().equals(this.configGroup.getTripsSheetName());
        List<DrtTrip> trips = this.getPerformedRequestSequences()
                .values()
                .stream()
                .filter(PerformedRequestEventSequence::isCompleted)
                .map(sequence -> new DrtTrip(sequence, network.getLinks()::get, this.getScheme()))
                .collect(toList());

        Row row = sheet.createRow(0);
        for (int i = 0; i< TRIPS_HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(TRIPS_HEADERS[i]);
        }

        for(int i=0; i<trips.size(); i++) {
            row = sheet.createRow(i+1);
            Cell cell = row.createCell(0);
            cell.setCellValue(trips.get(i).departureTime);
            cell = row.createCell(1);
            cell.setCellValue(trips.get(i).person.toString());
            cell = row.createCell(2);
            cell.setCellValue(trips.get(i).vehicle.toString());
            cell = row.createCell(3);
            cell.setCellValue(trips.get(i).fromLinkId.toString());
            cell = row.createCell(4);
            cell.setCellValue(trips.get(i).fromCoord.getX());
            cell = row.createCell(5);
            cell.setCellValue(trips.get(i).fromCoord.getY());
            cell = row.createCell(6);
            cell.setCellValue(trips.get(i).toLink.toString());
            cell = row.createCell(7);
            cell.setCellValue(trips.get(i).toCoord.getX());
            cell = row.createCell(8);
            cell.setCellValue(trips.get(i).toCoord.getY());
            cell = row.createCell(9);
            cell.setCellValue(trips.get(i).waitTime);
            cell = row.createCell(10);
            cell.setCellValue(trips.get(i).arrivalTime);
            cell = row.createCell(11);
            cell.setCellValue(trips.get(i).arrivalTime - trips.get(i).departureTime);
            cell = row.createCell(12);
            cell.setCellValue(trips.get(i).unsharedDistanceEstimate_m);
            cell = row.createCell(13);
            cell.setCellValue(trips.get(i).traveledDistance);
            cell = row.createCell(14);
            cell.setCellValue(trips.get(i).fare);
            cell = row.createCell(15);
            cell.setCellValue(trips.get(i).beforeActivityType + "->" + trips.get(i).afterActivityType);
            cell = row.createCell(16);
            cell.setCellValue(trips.get(i).accessTime);
            cell = row.createCell(17);
            cell.setCellValue(trips.get(i).egressTime);
        }
    }

    public void fillVehicles(Sheet sheet) {
        assert sheet.getSheetName().equals(this.configGroup.getVehiclesSheetName());
        Row row = sheet.createRow(0);
        for (int i = 0; i< VEHICLES_HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(VEHICLES_HEADERS[i]);
        }
        int rowCount = 1;
        for(Id<Vehicle> vehicleId : this.vehiclesDistances.keySet()) {
            row = sheet.createRow(rowCount);
            Cell cell = row.createCell(0);
            cell.setCellValue(vehicleId.toString());
            cell = row.createCell(1);
            cell.setCellValue(this.vehiclesDistances.get(vehicleId));
            rowCount+=1;
        }
    }

    public static class PerformedRequestEventSequence {
        private final DrtRequestSubmittedEvent submitted;
        private final PassengerRequestScheduledEvent scheduled;
        //pickedUp and droppedOff may be null if QSim ends before the request is actually handled
        @Nullable
        private PassengerPickedUpEvent pickedUp;
        @Nullable
        private PassengerDroppedOffEvent droppedOff;
        @Nullable
        private PersonMoneyEvent fare;
        private double distance = 0;

        private ActivityEndEvent beforeActivity;
        private ActivityStartEvent afterActivity;

        public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
                                             PassengerRequestScheduledEvent scheduled) {
            this(submitted, scheduled, null, null, null, null);
        }

        public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
                                             PassengerRequestScheduledEvent scheduled, ActivityEndEvent beforeActivity) {
            this(submitted, scheduled, null, null, beforeActivity, null);
        }

        public PerformedRequestEventSequence(DrtRequestSubmittedEvent submitted,
                                             PassengerRequestScheduledEvent scheduled, PassengerPickedUpEvent pickedUp,
                                             PassengerDroppedOffEvent droppedOff, ActivityEndEvent beforeActivity,
                                             ActivityStartEvent afterActivity) {
            this.submitted = Objects.requireNonNull(submitted);
            this.scheduled = Objects.requireNonNull(scheduled);
            this.pickedUp = pickedUp;
            this.droppedOff = droppedOff;
            this.distance = 0;
            this.beforeActivity = beforeActivity;
            this.afterActivity = afterActivity;
        }

        public DrtRequestSubmittedEvent getSubmitted() {
            return submitted;
        }

        public PassengerRequestScheduledEvent getScheduled() {
            return scheduled;
        }

        public Optional<PassengerPickedUpEvent> getPickedUp() {
            return Optional.ofNullable(pickedUp);
        }

        public Optional<PassengerDroppedOffEvent> getDroppedOff() {
            return Optional.ofNullable(droppedOff);
        }

        public Optional<PersonMoneyEvent> getFare() {
            return Optional.ofNullable(fare);
        }

        public ActivityEndEvent getBeforeActivity() {
            return this.beforeActivity;
        }

        public ActivityStartEvent getAfterActivity() {
            return this.afterActivity;
        }

        public double getDistance() {
            return this.distance;
        }

        public boolean isCompleted() {
            return droppedOff != null;
        }
    }

    public static class RejectedRequestEventSequence {
        private final DrtRequestSubmittedEvent submitted;
        private final PassengerRequestRejectedEvent rejected;

        public RejectedRequestEventSequence(DrtRequestSubmittedEvent submitted,
                                            PassengerRequestRejectedEvent rejected) {
            this.submitted = submitted;
            this.rejected = rejected;
        }

        public DrtRequestSubmittedEvent getSubmitted() {
            return submitted;
        }

        public PassengerRequestRejectedEvent getRejected() {
            return rejected;
        }
    }
}
