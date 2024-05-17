package org.eqasim.core.analysis.cba.analyzers.genericAnalysis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

import java.util.*;

public class GenericAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, TeleportationArrivalEventHandler, MobsimScopeEventHandler, CbaAnalyzer {

    private static final String[] TRIPS_HEADERS = new String[]{"personID", "purpose", "travelDuration", "travelDistance", "departureTime", "arrivalTime"};

    private final Map<Id<Person>, PersonArrivalEvent> agentArrivals = new HashMap<>();
    private final Map<Id<Person>, GenericTrip> currentTrips = new HashMap<>();
    private final GenericAnalyzerConfigGroup configGroup;
    private final List<GenericTrip> trips = new ArrayList<>();


    private final String[] sheetsNames;

    public GenericAnalyzer(GenericAnalyzerConfigGroup configGroup) {
        this.configGroup = configGroup;
        this.sheetsNames = new String[]{configGroup.getTripsSheetName()};
    }


    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().endsWith(" interaction")) {
            return;
        }
        GenericTrip trip = new GenericTrip(event.getPersonId());
        this.currentTrips.put(event.getPersonId(), trip);
        trip.previousActivityEnd = event;
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        GenericTrip trip = currentTrips.getOrDefault(event.getPersonId(), null);
        if(trip == null) {
            return;
        }
        if(!event.getLegMode().equals(this.configGroup.getMode())) {
            currentTrips.remove(event.getPersonId());
        }
        else
        {
            trip.departureEvent = event;
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        GenericTrip trip = currentTrips.getOrDefault(event.getPersonId(), null);
        if (trip != null) {
            trip.teleportationArrivalEvent = event;
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        GenericTrip trip = currentTrips.getOrDefault(event.getPersonId(), null);
        if (trip != null) {
            this.agentArrivals.put(event.getPersonId(), event);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().endsWith(" interaction")) {
            return;
        }
        GenericTrip trip = currentTrips.getOrDefault(event.getPersonId(), null);
        // Need to check if departureEvent & teleportationArrivalEvent are present
        // Because drt drivers can go from beforeDvrpSchedule activity to DrtStay activity without a departure between them
        if(trip != null && trip.departureEvent != null && trip.teleportationArrivalEvent != null) {
            trip.nextActivityStart = event;
            this.trips.add(trip);
        }
    }



    @Override
    public String[] getSheetsNames() {
        return this.sheetsNames;
    }

    @Override
    public void fillSheets(List<Sheet> sheets) {
        assert sheets.size() >= sheetsNames.length;
        Sheet tripsSheet = sheets.get(0);
        assert tripsSheet.getSheetName().equals(this.sheetsNames[0]);
        Row row = tripsSheet.createRow(0);

        for (int i = 0; i< TRIPS_HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(TRIPS_HEADERS[i]);
        }
        int rowCounter = 1;
        for(int i=0; i<trips.size(); i++) {
            row = tripsSheet.createRow(rowCounter);
            rowCounter++;
            Cell cell = row.createCell(0);
            cell.setCellValue(trips.get(i).personId.toString());
            cell = row.createCell(1);
            cell.setCellValue(trips.get(i).getPurpose().replaceAll("[0-9]|_", ""));
            cell = row.createCell(2);
            cell.setCellValue(trips.get(i).teleportationArrivalEvent.getTime() - trips.get(i).departureEvent.getTime());
            cell = row.createCell(3);
            cell.setCellValue(trips.get(i).teleportationArrivalEvent.getDistance());
            cell = row.createCell(4);
            cell.setCellValue(trips.get(i).departureEvent.getTime());
            cell = row.createCell(5);
            cell.setCellValue(trips.get(i).nextActivityStart.getTime());
        }
    }


}
