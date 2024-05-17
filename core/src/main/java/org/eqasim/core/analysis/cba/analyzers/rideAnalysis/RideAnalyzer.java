package org.eqasim.core.analysis.cba.analyzers.rideAnalysis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideAnalyzer implements ActivityEndEventHandler, PersonDepartureEventHandler, TeleportationArrivalEventHandler, PersonArrivalEventHandler, ActivityStartEventHandler, MobsimScopeEventHandler, CbaAnalyzer {

    private final RideAnalyzerConfigGroup configGroup;
    private final Map<Id<Person>, RideTrip> currentTrips = new HashMap<>();
    private final List<RideTrip> trips = new ArrayList<>();
    private final String[] sheetsNames = new String[1];
    private static final String[] TRIPS_HEADERS = new String[]{"personId", "purpose", "departureTime", "arrivalTime", "accessTime", "egressTime", "rideTime", "accessDistance", "egressDistance", "rideDistance"};

    public RideAnalyzer(RideAnalyzerConfigGroup configGroup) {
        this.configGroup = configGroup;
        this.sheetsNames[0] = configGroup.getTripsSheetName();
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().endsWith(" interaction")) {
            RideTrip trip = this.currentTrips.get(event.getPersonId());
            trip.interactionActivitiesEndEvents.add(event);
        }
        else {
            RideTrip trip = new RideTrip();
            trip.previousActivityEnd = event;
            trip.personId = event.getPersonId();
            this.currentTrips.put(event.getPersonId(), trip);
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        RideTrip trip = currentTrips.get(event.getPersonId());
        if(trip != null) {
            trip.personDepartureEvents.add(event);
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        RideTrip trip = currentTrips.get(event.getPersonId());
        if(trip != null) {
            trip.teleportationArrivalEvents.add(event);
        }
    }

    @Override
    public void handleEvent(PersonArrivalEvent event) {
        RideTrip trip = currentTrips.get(event.getPersonId());
        if(trip != null) {
            trip.personArrivalEvents.add(event);
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().endsWith(" interaction")) {
            return;
        }
        RideTrip trip = currentTrips.getOrDefault(event.getPersonId(), null);
        // Need to check if departureEvent & teleportationArrivalEvent are present
        // Because drt drivers can go from beforeDvrpSchedule activity to DrtStay activity without a departure between them
        if(trip != null) {
            trip.nextActivityStart = event;
            if(trip.validate()) {
                this.trips.add(trip);
            }
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
            //"personId", "purpose", "departureTime", "arrivalTime", "accessTime", "egressTime", "rideTime", "accessDistance", "egressDistance", "rideDistance"
            row = tripsSheet.createRow(rowCounter);
            rowCounter++;
            Cell cell = row.createCell(0);
            cell.setCellValue(trips.get(i).personId.toString());
            cell = row.createCell(1);
            cell.setCellValue(trips.get(i).getPurpose().replaceAll("[0-9]|_", ""));
            cell = row.createCell(2);
            cell.setCellValue(trips.get(i).getDepartureTime());
            cell = row.createCell(3);
            cell.setCellValue(trips.get(i).getArrivalTime());
            cell = row.createCell(4);
            cell.setCellValue(trips.get(i).getAccessTime());
            cell = row.createCell(5);
            cell.setCellValue(trips.get(i).getEgressTime());
            cell = row.createCell(6);
            cell.setCellValue(trips.get(i).getRideTime());
            cell = row.createCell(7);
            cell.setCellValue(trips.get(i).getAccessDistance());
            cell = row.createCell(8);
            cell.setCellValue(trips.get(i).getEgressDistance());
            cell = row.createCell(9);
            cell.setCellValue(trips.get(i).getRideDistance());
        }
    }
}
