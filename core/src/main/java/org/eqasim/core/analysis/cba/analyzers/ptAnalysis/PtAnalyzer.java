package org.eqasim.core.analysis.cba.analyzers.ptAnalysis;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.eqasim.core.analysis.cba.analyzers.CbaAnalyzer;
import org.eqasim.core.analysis.cba.utils.Tuple;
import org.eqasim.core.components.transit.events.PublicTransitEvent;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.experimental.events.TeleportationArrivalEvent;
import org.matsim.core.api.experimental.events.handler.TeleportationArrivalEventHandler;
import org.matsim.core.events.MobsimScopeEventHandler;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.misc.OptionalTime;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.vehicles.Vehicle;

import java.util.*;
import java.util.stream.Collectors;

/*
TODO Distances:
 - Distance per person
 - Distance per vehicle (distinguished per mode)
    - Establish a vehicles to modes map with the hypothesis that one vehicle can serve only one mode through the simulation
 - Make sure to get the actual performed distance & not the planned one
 - Access time to PT, DRT.... use the "X interaction" mode
    - Trip is composed of : access - wait - in vehicle - egress
 - Applied Fares
    - Not grave if two fares of the same mode are swapped
    - Can begin without the pt fare

 */

//TODO Follow VehicleEntersLink & VehicleLeavesLink events to compute the travelled distance
public class PtAnalyzer implements PersonDepartureEventHandler, PersonArrivalEventHandler, ActivityEndEventHandler, ActivityStartEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, LinkLeaveEventHandler, TeleportationArrivalEventHandler, TransitDriverStartsEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler, MobsimScopeEventHandler, GenericEventHandler, CbaAnalyzer {

    private static final String[] TRIPS_HEADERS = new String[]{"personID", "purpose", "inVehicleTime", "totalWaitingTime", "accessTime", "egressTime", "transferTime", "segmentIndex", "mode", "vehicleId", "pt_line", "pt_mode", "segmentAccessTime", "segmentEgressTime", "waitingTime", "travelDuration", "travelDistance"};
    private static final String[] VEHICLES_HEADERS = new String[]{"vehicleID", "lineID", "mode", "departureTime", "arrivalTime", "totalDistance"};

    private final IdMap<Vehicle, String> vehiclesToModes = new IdMap<>(Vehicle.class);
    private final IdMap<Person, PersonDepartureEvent> agentDepartures = new IdMap<>(Person.class);
    private final IdMap<Person, PersonArrivalEvent> agentArrivals = new IdMap<>(Person.class);
    private final IdMap<Person, List<Tuple<Tuple<PersonEntersVehicleEvent, Double>, PersonLeavesVehicleEvent>>> agentsInVehicles = new IdMap<>(Person.class);
    private final IdMap<Vehicle, Double> vehiclesDistances = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Set<Id<Person>>> vehiclesToPassengers = new IdMap<>(Vehicle.class);
    private final IdMap<Person, ActivityEndEvent> previousActivities = new IdMap<>(Person.class);
    private final IdMap<Person, ActivityEndEvent> previousNonInteractionActivities = new IdMap<>(Person.class);
    private final IdMap<Person, PtTrip> currentPtTrips = new IdMap<>(Person.class);
    private final Network network;
    private final PtAnalyzerConfigGroup configGroup;
    private final Scenario scenario;
    private final IdMap<Vehicle, Id<TransitLine>> vehiclesToTransitLines = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Id<TransitRoute>> vehiclesToTransitRoutes = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Double> vehiclesToDepartureTimes = new IdMap<>(Vehicle.class);
    private final IdMap<Vehicle, Double> vehiclesToArrivalTimes = new IdMap<>(Vehicle.class);
    List<PtTrip> trips = new ArrayList<>();

    private final String[] sheetsNames;
    private final Set<String> modes;

    public PtAnalyzer(PtAnalyzerConfigGroup configGroup, Network network, Scenario scenario) {
        this.configGroup = configGroup;
        this.network = network;
        if(configGroup.getVehiclesSheetName() != null) {
            this.sheetsNames = new String[]{configGroup.getTripsSheetName(), configGroup.getVehiclesSheetName()};
        } else {
            this.sheetsNames = new String[]{configGroup.getTripsSheetName()};
        }

        this.scenario = scenario;
        this.modes = Arrays.stream(this.configGroup.getMode().split(",")).collect(Collectors.toSet());

        Map<Id<Link>, ? extends Link> links = network.getLinks();

        for(TransitLine transitLine : this.scenario.getTransitSchedule().getTransitLines().values()) {
            for(TransitRoute transitRoute: transitLine.getRoutes().values()) {
                double routeDuration = transitRoute.getStops().get(transitRoute.getStops().size()-1).getArrivalOffset().seconds();
                double routeDistance = transitRoute.getRoute().getLinkIds().stream().map(links::get).mapToDouble(Link::getLength).sum();
                for(Departure departure: transitRoute.getDepartures().values()) {
                    this.vehiclesToTransitLines.put(departure.getVehicleId(), transitLine.getId());
                    this.vehiclesToTransitRoutes.put(departure.getVehicleId(), transitRoute.getId());
                    this.vehiclesToDepartureTimes.put(departure.getVehicleId(), departure.getDepartureTime());
                    this.vehiclesToArrivalTimes.put(departure.getVehicleId(), routeDuration);
                    this.vehiclesDistances.put(departure.getVehicleId(), routeDistance);
                }
            }
        }
    }


    @Override
    public void handleEvent(PersonArrivalEvent event) {
        // TODO Transport mode
        // event.getLegMode()
        // TODO Travel time with offset between departure & arrival
        this.agentArrivals.put(event.getPersonId(), event);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        this.previousActivities.put(event.getPersonId(), event);
        if (TripStructureUtils.isStageActivityType(event.getActType())) {
            return;
        }
        this.previousNonInteractionActivities.put(event.getPersonId(), event);
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (TripStructureUtils.isStageActivityType(event.getActType())) {
            return;
        }
        PtTrip trip = currentPtTrips.remove(event.getPersonId());
        if(trip == null) {
            return;
        }
        trip.nextActivityStart = event;
        for(PtTrip.TripSegment segment : trip.segments) {
            // We check that there's at least a PT leg, cause in cut simulations we can have trips with pt computational routing but no pt legs
            if(segment.mode.equals(TransportMode.pt)){
                trips.add(trip);
                return;
            }
        }
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        ActivityEndEvent activityEndEvent = this.previousActivities.get(event.getPersonId());
        if(this.modes.contains(event.getRoutingMode())) {
            this.agentDepartures.put(event.getPersonId(), event);
            PtTrip trip;
            if(activityEndEvent == this.previousNonInteractionActivities.get(event.getPersonId())) {
                trip = new PtTrip(event.getPersonId());
                this.currentPtTrips.put(event.getPersonId(), trip);
                trip.previousActivityEnd = activityEndEvent;
            } else {
                trip = this.currentPtTrips.get(event.getPersonId());
                assert trip != null;
            }
            trip.segments.add(new PtTrip.TripSegment(event.getLegMode(), null, 0, 0, 0));
            this.agentsInVehicles.put(event.getPersonId(), new ArrayList<>());
        } else {
            this.agentDepartures.remove(event.getPersonId());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        PersonDepartureEvent personDeparture = this.agentDepartures.get(event.getPersonId());
        if(personDeparture != null) {
            PtTrip trip = this.currentPtTrips.get(event.getPersonId());
            if(trip != null) {
                PtTrip.TripSegment segment = trip.getLastSegment();
                segment.vehicleId = event.getVehicleId();
                segment.travelTime = event.getTime();
                segment.waitingTime = event.getTime() - personDeparture.getTime();
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
        PtTrip trip = currentPtTrips.get(event.getPersonId());
        if(trip != null){
            PtTrip.TripSegment segment = trip.getLastSegment();
            if(segment != null && segment.vehicleId.equals(event.getVehicleId())) {
                segment.travelTime = event.getTime() - segment.travelTime;
            } else {
                throw new IllegalStateException("Shouldn't get here");
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
        if(this.scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())) {
            this.vehiclesDistances.put(event.getVehicleId(), this.network.getLinks().get(event.getLinkId()).getLength() + this.vehiclesDistances.getOrDefault(event.getVehicleId(), 0.0));
        }
        for(Id<Person> personId : this.vehiclesToPassengers.getOrDefault(event.getVehicleId(), new HashSet<>())) {
            PtTrip trip = this.currentPtTrips.get(personId);
            if (trip != null) {
                PtTrip.TripSegment segment = this.currentPtTrips.get(personId).getLastSegment();
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
            PtTrip trip = trips.get(i);
            for (int j=0; j< trips.get(i).segments.size(); j++) {
                PtTrip.TripSegment segment = trip.segments.get(j);
                row = tripsSheet.createRow(rowCounter);
                rowCounter++;
                //"personID", "purpose", "inVehicleTime", "totalWaitingTime", "accessTime", "egressTime", "transferTime", "segmentIndex", "mode", "vehicleId", "pt_line", "pt_mode", "segmentAccessTime", "segmentEgressTime", "waitingTime", "travelDuration", "travelDistance"
                Cell cell = row.createCell(0);
                cell.setCellValue(trip.personId.toString());
                cell = row.createCell(1);
                cell.setCellValue(trip.getPurpose().replaceAll("[0-9]|_", ""));
                cell = row.createCell(2);
                cell.setCellValue(trip.getInVehicleTime());
                cell = row.createCell(3);
                cell.setCellValue(trip.getTotalWaitingTime());
                cell = row.createCell(4);
                cell.setCellValue(trip.getAccessTime());
                cell = row.createCell(5);
                cell.setCellValue(trip.getEgressTime());
                cell = row.createCell(6);
                cell.setCellValue(trip.getTransferTime());
                cell = row.createCell(7);
                cell.setCellValue(j);
                cell = row.createCell(8);
                cell.setCellValue(segment.mode);
                cell = row.createCell(9);
                Cell ptLineCell = row.createCell(10);
                Cell ptModeCell = row.createCell(11);
                Id<Vehicle> vehicleId = segment.vehicleId;
                if(vehicleId != null) {
                    cell.setCellValue(segment.vehicleId.toString());
                }
                if("pt".equals(segment.mode)) {
                    if(vehicleId != null && this.vehiclesToTransitLines.containsKey(vehicleId)) {
                        putTransitLineInfoInCell(vehicleId, ptLineCell);
                        ptModeCell.setCellValue(this.scenario.getTransitSchedule().getTransitLines().get(this.vehiclesToTransitLines.get(vehicleId)).getRoutes().get(this.vehiclesToTransitRoutes.get(vehicleId)).getTransportMode());
                    } else if (trips.get(i).segments.get(j).transitRouteId != null) {
                        writePtInfoFromTransitRoute(trips.get(i).segments.get(j).transitRouteId, this.scenario.getTransitSchedule(), ptLineCell, ptModeCell, cell, trips.get(i).segments.get(j).vehicleDepartureTime);
                    }
                }
                cell = row.createCell(12);
                cell.setCellValue(trips.get(i).getSegmentAccessTime(j));
                cell = row.createCell(13);
                cell.setCellValue(trips.get(i).getSegmentEgressTime(j));
                cell = row.createCell(14);
                cell.setCellValue(trips.get(i).segments.get(j).waitingTime);
                cell = row.createCell(15);
                cell.setCellValue(trips.get(i).segments.get(j).travelTime);
                cell = row.createCell(16);
                cell.setCellValue(trips.get(i).segments.get(j).distance);
            }
        }
        if(sheets.size() == 1) {
            return;
        }
        Sheet vehiclesSheet = sheets.get(1);
        assert vehiclesSheet.getSheetName().equals(this.sheetsNames[1]);
        row = vehiclesSheet.createRow(0);
        for (int i = 0; i< VEHICLES_HEADERS.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(VEHICLES_HEADERS[i]);
        }
        rowCounter = 1;
        for(Id<Vehicle> vehicleId : this.scenario.getTransitVehicles().getVehicles().keySet()) {
            row = vehiclesSheet.createRow(rowCounter);
            Cell cell = row.createCell(0);
            cell.setCellValue(vehicleId.toString());
            cell = row.createCell(1);
            putTransitLineInfoInCell(vehicleId, cell);
            cell = row.createCell(2);
            cell.setCellValue(this.scenario.getTransitSchedule().getTransitLines().get(this.vehiclesToTransitLines.get(vehicleId)).getRoutes().get(this.vehiclesToTransitRoutes.get(vehicleId)).getTransportMode());
            cell = row.createCell(3);
            if(this.vehiclesToDepartureTimes.containsKey(vehicleId)) {
                cell.setCellValue(this.vehiclesToDepartureTimes.get(vehicleId));
            }
            cell = row.createCell(4);
            if(this.vehiclesToArrivalTimes.containsKey(vehicleId)) {
                cell.setCellValue(this.vehiclesToArrivalTimes.get(vehicleId));
            }
            cell = row.createCell(5);
            if(this.vehiclesDistances.containsKey(vehicleId)){
                cell.setCellValue(this.vehiclesDistances.getOrDefault(vehicleId, 0.0));
            } else {
                putVehicleDistanceFromSchedule(vehicleId, this.scenario.getTransitSchedule(), cell);
            }
            rowCounter++;
        }
    }

    private void putVehicleDistanceFromSchedule(Id<Vehicle> vehicleId, TransitSchedule schedule, Cell cell) {
        for(TransitLine transitLine : schedule.getTransitLines().values()) {
            for(TransitRoute transitRoute : transitLine.getRoutes().values()) {
                for(Departure departure : transitRoute.getDepartures().values()) {
                    if(vehicleId.equals(departure.getVehicleId())) {
                        cell.setCellValue(transitRoute.getRoute().getDistance());
                    }
                }
            }
        }
    }

    public void writePtInfoFromTransitRoute(Id<TransitRoute> routeId, TransitSchedule schedule, Cell ptLineCell, Cell ptModeCell, Cell vehicleIdCell, Double departureTime) {
        for(TransitLine transitLine: schedule.getTransitLines().values()) {
            for(TransitRoute transitRoute : transitLine.getRoutes().values()){
                if(transitRoute.getId().equals(routeId)) {
                    for(Departure departure: transitRoute.getDepartures().values()) {
                        if(departure.getDepartureTime() == departureTime) {
                            vehicleIdCell.setCellValue(departure.getVehicleId().toString());
                            break;
                        }
                    }
                    ptLineCell.setCellValue(transitLine.getId().toString());
                    ptModeCell.setCellValue(transitRoute.getTransportMode());
                    return;
                }
            }
        }
    }

    private void putTransitLineInfoInCell(Id<Vehicle> vehicleId, Cell cell) {
        TransitLine transitLine = this.scenario.getTransitSchedule().getTransitLines().get(this.vehiclesToTransitLines.get(vehicleId));
        Object gtfsRouteShortName = transitLine.getAttributes().getAttribute("gtfs_route_short_name");
        if(gtfsRouteShortName != null) {
            cell.setCellValue(gtfsRouteShortName.toString());
        }
        else {
            cell.setCellValue(transitLine.getId().toString());
        }
    }

    @Override
    public void handleEvent(TeleportationArrivalEvent event) {
        PersonDepartureEvent departureEvent = this.agentDepartures.get(event.getPersonId());
        if(departureEvent==null) {
            return;
        }
        double time = event.getTime() - this.agentDepartures.get(event.getPersonId()).getTime();
        PtTrip trip = this.currentPtTrips.get(event.getPersonId());
        if(trip != null) {
            PtTrip.TripSegment segment = trip.getLastSegment();
            segment.travelTime = time;
            segment.distance = event.getDistance();
        }
    }

    @Override
    public void handleEvent(TransitDriverStartsEvent event) {
        this.vehiclesToTransitLines.put(event.getVehicleId(), event.getTransitLineId());
        this.vehiclesToTransitRoutes.put(event.getVehicleId(), event.getTransitRouteId());

    }

    @Override
    public void handleEvent(VehicleEntersTrafficEvent event) {
        if(this.scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())) {
            this.vehiclesToDepartureTimes.put(event.getVehicleId(), event.getTime());
            this.vehiclesDistances.put(event.getVehicleId(), 0.0);
        }
    }

    @Override
    public void handleEvent(VehicleLeavesTrafficEvent event) {
        if(this.scenario.getTransitVehicles().getVehicles().containsKey(event.getVehicleId())) {
            this.vehiclesToArrivalTimes.put(event.getVehicleId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(GenericEvent genericEvent) {
        if(!genericEvent.getEventType().equals("pt_transit")) {
            return;
        }
        Id<Person> personId = Id.createPersonId(genericEvent.getAttributes().get("person"));
        PtTrip trip = this.currentPtTrips.get(personId);
        if(trip == null) {
            return;
        }
        PtTrip.TripSegment segment = trip.getLastSegment();
        if(segment != null) {
            segment.transitRouteId = Id.create(genericEvent.getAttributes().get("route"), TransitRoute.class);
            segment.vehicleDepartureTime = Double.parseDouble(genericEvent.getAttributes().get("vehicleDepartureTime"));
            for(TransitLine transitLine: this.scenario.getTransitSchedule().getTransitLines().values()){
                for(TransitRoute transitRoute: transitLine.getRoutes().values()) {
                    if(transitRoute.getId().equals(segment.transitRouteId)){
                        OptionalTime stopDepartureOffset=null;
                        for(TransitRouteStop transitRouteStop: transitRoute.getStops()) {
                            if(transitRouteStop.getStopFacility().getId().toString().equals(genericEvent.getAttributes().get("accessStop"))) {
                                stopDepartureOffset = transitRouteStop.getDepartureOffset();
                            }
                        }
                        if(stopDepartureOffset!=null && stopDepartureOffset.isDefined()) {
                            for(Departure departure: transitRoute.getDepartures().values()) {
                                if(departure.getDepartureTime() == segment.vehicleDepartureTime-stopDepartureOffset.seconds()) {
                                    segment.vehicleId = departure.getVehicleId();
                                    segment.waitingTime = Double.parseDouble(genericEvent.getAttributes().get("boardingTime")) - this.agentDepartures.get(personId).getTime();
                                    this.vehiclesToTransitLines.put(segment.vehicleId,transitLine.getId());
                                    this.vehiclesToDepartureTimes.put(segment.vehicleId, departure.getDepartureTime());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
