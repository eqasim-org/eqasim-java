package org.eqasim.core.analysis.cba.analyzers.privateVehiclesAnalysis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.eqasim.core.analysis.cba.utils.Tuple;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class PrivateVehiclesAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, MobsimScopeEventHandler, CbaAnalyzer {

    private static final String[] TRIPS_HEADERS = new String[]{"personID", "purpose", "inVehicleTime", "accessTime", "egressTime", "transferTime", "segmentIndex", "mode", "vehicleId", "travelDuration", "travelDistance"};

    private final Map<Id<Person>, PersonDepartureEvent> agentDepartures = new HashMap();
    private final Map<Id<Person>, PersonArrivalEvent> agentArrivals = new HashMap<>();
    private final Map<Id<Person>, List<Tuple<Tuple<PersonEntersVehicleEvent, Double>, PersonLeavesVehicleEvent>>> agentsInVehicles = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehiclesDistances = new HashMap<>();
    private final Map<Id<Vehicle>, Set<Id<Person>>> vehiclesToPassengers = new HashMap<>();
    private final Map<Id<Person>, ActivityEndEvent> previousActivities = new HashMap<>();
    private final Map<Id<Person>, PrivateVehicleTrip> currentTrips = new HashMap<>();
    private final Network network;
    private final PrivateVehiclesAnalyzerConfigGroup configGroup;
    private final Map<Id<Vehicle>, Id<TransitLine>> vehiclesToTransitLines = new HashMap<>();
    private final Map<Id<Vehicle>, Id<TransitRoute>> vehiclesToTransitRoutes = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehiclesToDepartureTimes = new HashMap<>();
    private final Map<Id<Vehicle>, Double> vehiclesToArrivalTimes = new HashMap<>();
    List<PrivateVehicleTrip> trips = new ArrayList<>();

    private final String[] sheetsNames;

    public PrivateVehiclesAnalyzer(PrivateVehiclesAnalyzerConfigGroup configGroup, Network network) {
        this.configGroup = configGroup;
        this.network = network;
        this.sheetsNames = new String[]{configGroup.getTripsSheetName()};
    }


    @Override
    public void handleEvent(PersonArrivalEvent event) {
        this.agentArrivals.put(event.getPersonId(), event);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        this.previousActivities.put(event.getPersonId(), event);
        if(this.configGroup.getIgnoredActivityTypes().contains(event.getActType())) {
            return;
        }
        if (event.getActType().endsWith(" interaction")) {
            return;
        }
        PrivateVehicleTrip trip = new PrivateVehicleTrip(event.getPersonId());
        this.currentTrips.put(event.getPersonId(), trip);
        trip.previousActivityEnd = event;
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if(this.configGroup.getIgnoredActivityTypes().contains(event.getActType())) {
            return;
        }
        if (event.getActType().endsWith(" interaction")) {
            return;
        }
        PrivateVehicleTrip trip = currentTrips.get(event.getPersonId());
        trip.nextActivityStart = event;
        for(PrivateVehicleTrip.TripSegment segment : trip.segments) {
            if(this.configGroup.getModes().contains(segment.mode)){
                trips.add(trip);
                return;
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        this.agentDepartures.put(event.getPersonId(), event);
        this.agentsInVehicles.put(event.getPersonId(), new ArrayList<>());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PersonDepartureEvent personDeparture = this.agentDepartures.get(event.getPersonId());
        if(personDeparture != null) {
            PrivateVehicleTrip trip = this.currentTrips.get(event.getPersonId());
            if(trip != null) {
                trip.segments.add(new PrivateVehicleTrip.TripSegment(personDeparture.getLegMode(), event.getVehicleId(), event.getTime(), 0, event.getTime() - personDeparture.getTime()));
            }
            if(!this.agentsInVehicles.containsKey(event.getPersonId())) {
                this.agentsInVehicles.put(event.getPersonId(), new ArrayList<>());
            }
            this.agentsInVehicles.get(event.getPersonId()).add(new Tuple<>(new Tuple<>(event, 0.0), null));
            if(!this.vehiclesToPassengers.containsKey(event.getVehicleId())) {
                this.vehiclesToPassengers.put(event.getVehicleId(), new HashSet<>());
            }
            this.vehiclesToPassengers.get(event.getVehicleId()).add(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        PrivateVehicleTrip trip = currentTrips.get(event.getPersonId());
        if(trip !=null){
            PrivateVehicleTrip.TripSegment segment = trip.getLastSegment();
            if(segment != null && segment.vehicleId.equals(event.getVehicleId())) {
                segment.travelTime = event.getTime() - segment.travelTime;
            }
        }
        List<Tuple<Tuple<PersonEntersVehicleEvent, Double>, PersonLeavesVehicleEvent>> tuples = this.agentsInVehicles.getOrDefault(event.getPersonId(), new ArrayList<>());
        for(Tuple<Tuple<PersonEntersVehicleEvent, Double>, PersonLeavesVehicleEvent> tuple: tuples) {
            if(tuple.getFirst().getFirst().getVehicleId().equals(event.getVehicleId()) && tuple.getSecond() == null) {
                tuple.setSecond(event);
            }
        }
        if(this.vehiclesToPassengers.containsKey(event.getVehicleId())){
            this.vehiclesToPassengers.get(event.getVehicleId()).remove(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {

    }

    @Override
    public void handleEvent(LinkLeaveEvent event) {
        this.vehiclesDistances.put(event.getVehicleId(), this.network.getLinks().get(event.getLinkId()).getLength() + this.vehiclesDistances.getOrDefault(event.getVehicleId(), 0.0));
        for(Id<Person> personId : this.vehiclesToPassengers.getOrDefault(event.getVehicleId(), new HashSet<>())) {
            PrivateVehicleTrip trip = this.currentTrips.get(personId);
            if (trip != null) {
                PrivateVehicleTrip.TripSegment segment = this.currentTrips.get(personId).getLastSegment();
                if(segment != null && segment.vehicleId.equals(event.getVehicleId())) {
                    segment.distance += network.getLinks().get(event.getLinkId()).getLength();
                }
            }
            for(Tuple<Tuple<PersonEntersVehicleEvent,Double>, PersonLeavesVehicleEvent> inVehicle : this.agentsInVehicles.get(personId)) {
                if(inVehicle.getFirst().getFirst().getVehicleId().equals(event.getVehicleId())) {
                    inVehicle.getFirst().setSecond(inVehicle.getFirst().getSecond() + network.getLinks().get(event.getLinkId()).getLength());
                }
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
            for (int j=0; j< trips.get(i).segments.size(); j++) {
                Id<Vehicle> vehicleId = trips.get(i).segments.get(j).vehicleId;
                if(vehicleId == null) {
                    continue;
                }
                row = tripsSheet.createRow(rowCounter);
                rowCounter++;
                //"personID", "purpose", "inVehicleTime", "accessTime", "egressTime", "transferTime", "segmentIndex", "mode", "vehicleId", "waitingTime", "travelDuration", "travelDistance"
                Cell cell = row.createCell(0);
                cell.setCellValue(trips.get(i).personId.toString());
                cell = row.createCell(1);
                cell.setCellValue(trips.get(i).getPurpose().replaceAll("[0-9]|_", ""));
                cell = row.createCell(2);
                cell.setCellValue(trips.get(i).getInvehicleTime());
                cell = row.createCell(3);
                cell.setCellValue(trips.get(i).getAccessTime());
                cell = row.createCell(4);
                cell.setCellValue(trips.get(i).getEgressTime());
                cell = row.createCell(5);
                cell.setCellValue(trips.get(i).getTransferTime());
                cell = row.createCell(6);
                cell.setCellValue(j);
                cell = row.createCell(7);
                cell.setCellValue(trips.get(i).segments.get(j).mode);
                cell = row.createCell(8);
                if(vehicleId != null) {
                    cell.setCellValue(trips.get(i).segments.get(j).vehicleId.toString());
                }
                cell = row.createCell(9);
                cell.setCellValue(trips.get(i).segments.get(j).travelTime);
                cell = row.createCell(10);
                cell.setCellValue(trips.get(i).segments.get(j).distance);
                break;
            }
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        double time = event.getTime() - this.agentDepartures.get(event.getPersonId()).getTime();
        this.currentTrips.get(event.getPersonId()).segments.add(new PrivateVehicleTrip.TripSegment(event.getMode(), null, time, event.getDistance(), 0));
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        this.vehiclesToTransitLines.put(event.getVehicleId(), event.getTransitLineId());
        this.vehiclesToTransitRoutes.put(event.getVehicleId(), event.getTransitRouteId());

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        this.vehiclesToDepartureTimes.put(event.getVehicleId(), event.getTime());
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        this.vehiclesToArrivalTimes.put(event.getVehicleId(), event.getTime());

    }
}
