package org.eqasim.core.simulation.modes.parking_aware_car.routing;

import org.eqasim.core.simulation.modes.parking_aware_car.definitions.ParkingSpace;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.Facility;

public class InitialParkingAssignment implements IterationStartsListener {

    public static final String INITIAL_VEHICLE_LOCATION_ATTRIBUTE = "initialVehicleLocation";

    private final ParkingSpaceFinder parkingSpaceFinder;
    private final Population population;
    private final ActivityFacilities activityFacilities;

    public InitialParkingAssignment(Population population, ParkingSpaceFinder parkingSpaceFinder, ActivityFacilities activityFacilities) {
        this.population = population;
        this.parkingSpaceFinder = parkingSpaceFinder;
        this.activityFacilities = activityFacilities;
    }

    @Override
    public void notifyIterationStarts(IterationStartsEvent event) {
        for(Person person: population.getPersons().values()) {
            for(PlanElement planElement: person.getSelectedPlan().getPlanElements()) {
                if(planElement instanceof Activity activity && !TripStructureUtils.isStageActivityType(activity.getType())) {
                    Facility facility = activityFacilities.getFacilities().get(activity.getFacilityId());
                    if(facility == null) {
                        throw new IllegalStateException(String.format("Could not find facility %s for activity %s of person %s", activity.getFacilityId().toString(), activity.getType(), person.getId().toString()));
                    }
                    ParkingSpace parkingSPace = this.parkingSpaceFinder.findParkingSpace(person, facility, 0);
                    person.getAttributes().putAttribute(INITIAL_VEHICLE_LOCATION_ATTRIBUTE, parkingSPace.linkId());
                    break;
                }
            }
         }
    }
}
