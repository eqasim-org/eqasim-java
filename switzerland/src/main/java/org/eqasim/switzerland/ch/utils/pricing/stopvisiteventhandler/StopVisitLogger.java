package org.eqasim.switzerland.ch.utils.pricing.stopvisiteventhandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.ZonalRegistry;
import org.eqasim.switzerland.ch.utils.pricing.inputs.zonal.Zone;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;

public class StopVisitLogger implements PersonEntersVehicleEventHandler,PersonLeavesVehicleEventHandler, VehicleArrivesAtFacilityEventHandler {

    private final String personId;
    private final ZonalRegistry zonalRegistry;
    private final Set<String> visitedStops = new HashSet<>();
    private String vehicleOfPerson = null;
    private final Map<String, String> vehicleLastStop = new HashMap<>();

    public StopVisitLogger(String personId, ZonalRegistry zonalRegistry) {
        this.personId = personId;
        this.zonalRegistry = zonalRegistry;
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        if (event.getPersonId().toString().equals(personId)) {
            vehicleOfPerson = event.getVehicleId().toString();

            // Get the last stop this vehicle visited
            String stop = vehicleLastStop.get(vehicleOfPerson);
            if (stop != null) {
                visitedStops.add(stop);
                String stopId = stop.split(".link")[0];
                Collection<Zone> zones = zonalRegistry.getZones(stopId);
                if (zones == null || zones.isEmpty()) {
                    String baseStopId = stopId.split(":")[0];
                    zones = zonalRegistry.getZones(baseStopId);
                }
                System.out.println("Person " + personId + " boarded at stop: " + stop + " in zone " + zones);
            }
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        if (event.getVehicleId().toString().equals(vehicleOfPerson)) {
            String lastStop = vehicleLastStop.get(event.getVehicleId().toString());
            String stopId = lastStop.split(".link")[0];
            Collection<Zone> zones = zonalRegistry.getZones(stopId);
            if (zones == null || zones.isEmpty()) {
                String baseStopId = stopId.split(":")[0];
                zones = zonalRegistry.getZones(baseStopId);
            }
            System.out.println("Person " + personId + " alighted at stop: " + lastStop + " in zone " + zones);
            vehicleOfPerson = null;
        }
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        vehicleLastStop.put(event.getVehicleId().toString(), event.getFacilityId().toString());
        // If the person is on this vehicle, log the facility
        if (event.getVehicleId().toString().equals(vehicleOfPerson)) {
            visitedStops.add(event.getFacilityId().toString());
            String eventFacility = event.getFacilityId().toString();
            String stopId = eventFacility.split(".link")[0];
            Collection<Zone> zones = zonalRegistry.getZones(stopId);
            if (zones == null || zones.isEmpty()) {
                String baseStopId = stopId.split(":")[0];
                zones = zonalRegistry.getZones(baseStopId);
            }
            System.out.println("Person " + personId + " passed stop: " + event.getFacilityId() + " in zone " + zones);
        }
    }

    @Override
    public void reset(int iteration) {
        visitedStops.clear();
        vehicleOfPerson = null;
        vehicleLastStop.clear();
    }

    public Set<String> getVisitedStops() {
        return visitedStops;
    }
    
}
