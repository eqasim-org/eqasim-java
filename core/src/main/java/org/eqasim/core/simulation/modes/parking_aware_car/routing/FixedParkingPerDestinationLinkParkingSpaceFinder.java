package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.scenario.cutter.extent.ScenarioExtent;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.NetworkWideParkingSpaceStore;
import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.eqasim.core.simulation.modes.parking_aware_car.handlers.ParkingUsageEventListener;
import org.eqasim.core.simulation.modes.parking_aware_car.parking_assignment.ParkingSpaceAssignmentLogic;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.Facility;

import java.util.Set;

public class FixedParkingPerDestinationLinkParkingSpaceFinder extends DefaultParkingSpaceFinder{

    private final Set<String> activityTypes;

    public FixedParkingPerDestinationLinkParkingSpaceFinder(NetworkWideParkingSpaceStore networkWideParkingSpaceStore, Network network, ParkingSpaceAssignmentLogic parkingSpaceAssignmentLogic, ParkingUsageEventListener parkingUsageEventListener, ScenarioExtent scenarioExtent, double assumedParkingDuration, int searchRadius, Set<String> activityTypes) {
        super(networkWideParkingSpaceStore, network, parkingSpaceAssignmentLogic, parkingUsageEventListener, scenarioExtent, assumedParkingDuration, searchRadius);
        this.activityTypes = activityTypes;
    }

    public ParkingSpace findParkingSpace(Person person, Facility facility, double parkingStartTime) {
        String attributeName = "parking:"+facility.getLinkId().toString();
        String linkId = (String) person.getAttributes().getAttribute(attributeName);
        if(linkId != null) {
            return this.getParkingUsageLogic().getUsedParkingSpace(getNetworkWideParkingSpaceStore(), person.getId(), Id.createLinkId(linkId));
        }
        if(facility instanceof ActivityFacility activityFacility) {
            for(String activityType : activityFacility.getActivityOptions().keySet()) {
                if(activityTypes.isEmpty() || activityTypes.contains(activityType)) {
                    ParkingSpace parkingSpace = super.findParkingSpace(person, facility, parkingStartTime);
                    person.getAttributes().putAttribute(attributeName, parkingSpace.linkId().toString());
                    return parkingSpace;
                }
            }
        }
        return super.findParkingSpace(person, facility, parkingStartTime);
    }
}
