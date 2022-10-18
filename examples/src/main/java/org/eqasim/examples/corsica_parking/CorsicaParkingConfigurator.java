package org.eqasim.examples.corsica_parking;

import org.eqasim.ile_de_france.IDFConfigurator;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.ParkingUtils;
import org.matsim.contrib.parking.parkingsearch.manager.facilities.ParkingFacilityType;
import org.matsim.facilities.*;
import org.matsim.households.Household;
import org.matsim.vehicles.Vehicle;

import java.util.*;

public class CorsicaParkingConfigurator extends IDFConfigurator {

    private int randomSeed = 1;
    private double probabilityParkingAtHome = 0.9;
    private double probabilityParkingAtWork = 0.5;

    private double probabilityOnStreetParkingOnLink = 0.9;
    private int minOnStreetParkingCapacity = 1;
    private int maxOnStreetParkingCapacity = 10;

    private double probabilityGarageParkingOnLink = 0.01;
    private int minGarageParkingCapacity = 10;
    private int maxGarageParkingCapacity = 1000;

    @Override
    public void adjustScenario(Scenario scenario) {
        super.adjustScenario(scenario);

        // get activity facilities
        ActivityFacilities activityFacilities = scenario.getActivityFacilities();
        ActivityFacilitiesFactory activityFacilitiesFactory = activityFacilities.getFactory();

        Random random = new Random(randomSeed);

        // add parking at home and at work
        for (Household household : scenario.getHouseholds().getHouseholds().values()) {
            boolean hasParkingAtHome = random.nextDouble() < probabilityParkingAtHome;

            for (Id<Person> personId : household.getMemberIds()) {
                boolean hasParkingAtWork = random.nextDouble() < probabilityParkingAtWork;

                Leg previousLeg = null;

                for (PlanElement element : scenario.getPopulation().getPersons().get(personId).getSelectedPlan().getPlanElements()) {
                    if (element instanceof Activity) {

                        // default parking search strategy for all car legs
                        if (previousLeg != null) {
                            if (previousLeg.getMode().equals("car")) {
                                previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.Random.toString());
                            }
                        }

                        if (((Activity) element).getType().equals("home")) {

                            element.getAttributes().putAttribute("parkingAvailable", hasParkingAtHome);

                            if (hasParkingAtHome) {

                                String parkingId = "dedicated_parking_facility_" + ((Activity) element).getFacilityId().toString();
                                Id<ActivityFacility> activityFacilityId = Id.create(parkingId, ActivityFacility.class);
                                element.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);

                                if (!activityFacilities.getFacilities().containsKey(activityFacilityId)) {

                                    // create parking facility
                                    ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(activityFacilityId,
                                            ((Activity) element).getCoord(), ((Activity) element).getLinkId());

                                    // add parking activity type as an option with capacity
                                    ActivityOption activityOption = activityFacilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
                                    activityOption.setCapacity(Integer.MAX_VALUE);
                                    activityFacility.addActivityOption(activityOption);

                                    // add parking facility type as an option to allow future filtering
                                    ActivityOption parkingFacilityTypeOption = activityFacilitiesFactory.createActivityOption(ParkingFacilityType.DedicatedParking.toString());
                                    activityFacility.addActivityOption(parkingFacilityTypeOption);

                                    // add facility attributes (type, allowed vehicles)
                                    activityFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.DedicatedParking.toString());
                                    Set<Id<Vehicle>> allowedVehicles = new HashSet<>();
                                    allowedVehicles.add(Id.createVehicleId(personId));
                                    activityFacility.getAttributes().putAttribute("allowedVehicles", allowedVehicles);

                                    // add facility to facilities
                                    activityFacilities.addActivityFacility(activityFacility);

                                } else {
                                    // add vehicle id to set of vehicle ids
                                    ((Set<Id<Vehicle>>) activityFacilities.getFacilities().get(activityFacilityId).getAttributes().getAttribute("allowedVehicles")).add(Id.createVehicleId(personId));
                                }

                                if (previousLeg != null) {
                                    if (previousLeg.getMode().equals("car")) {
                                        previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.DriveToParkingFacility.toString());
                                        previousLeg.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);
                                    }
                                }
                            }

                        }
                        else if (((Activity) element).getType().equals("work")) {
                            element.getAttributes().putAttribute("parkingAvailable", hasParkingAtWork);

                            if (hasParkingAtWork) {

                                String parkingId = "dedicated_parking_facility_" + ((Activity) element).getFacilityId().toString();
                                Id<ActivityFacility> activityFacilityId = Id.create(parkingId, ActivityFacility.class);
                                element.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);

                                if (!activityFacilities.getFacilities().containsKey(activityFacilityId)) {

                                    // create parking facility
                                    ActivityFacility activityFacility = activityFacilitiesFactory.createActivityFacility(activityFacilityId,
                                            ((Activity) element).getCoord(), ((Activity) element).getLinkId());

                                    // add parking activity type as an option with capacity
                                    ActivityOption activityOption = activityFacilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
                                    activityOption.setCapacity(Integer.MAX_VALUE);
                                    activityFacility.addActivityOption(activityOption);

                                    // add parking facility type as an option to allow future filtering
                                    ActivityOption parkingFacilityTypeOption = activityFacilitiesFactory.createActivityOption(ParkingFacilityType.DedicatedParking.toString());
                                    activityFacility.addActivityOption(parkingFacilityTypeOption);

                                    // add facility attributes (type, allowed vehicles)
                                    activityFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.DedicatedParking.toString());
                                    Set<Id<Vehicle>> allowedVehicles = new HashSet<>();
                                    allowedVehicles.add(Id.createVehicleId(personId));
                                    activityFacility.getAttributes().putAttribute("allowedVehicles", allowedVehicles);

                                    // add facility to facilities
                                    activityFacilities.addActivityFacility(activityFacility);
                                } else {
                                    // add vehicle id to set of vehicle ids
                                    ((Set<Id<Vehicle>>) activityFacilities.getFacilities().get(activityFacilityId).getAttributes().getAttribute("allowedVehicles")).add(Id.createVehicleId(personId));
                                }

                                if (previousLeg != null) {
                                    if (previousLeg.getMode().equals("car")) {
                                        previousLeg.getAttributes().putAttribute("parkingSearchStrategy", ParkingSearchStrategy.DriveToParkingFacility.toString());
                                        previousLeg.getAttributes().putAttribute("parkingFacilityId", activityFacilityId);
                                    }
                                }
                            }
                        }
                    }
                    else if (element instanceof Leg) {
                        previousLeg = (Leg) element;
                    }
                }
            }
        }

        // remove persons with only one activity
        List<Id<Person>> personIds = new LinkedList<>();
        for (Person person : scenario.getPopulation().getPersons().values()){
            if (person.getSelectedPlan().getPlanElements().size() == 1) {
                personIds.add(person.getId());
            }
        }
        for (Id<Person> personId : personIds){
            scenario.getPopulation().removePerson(personId);
        }

        // create blue-zone parking spaces and parking garage facility on each link
        for (Link link : scenario.getNetwork().getLinks().values()) {
            if (link.getAllowedModes().contains("car")) {

                if (random.nextDouble() < probabilityOnStreetParkingOnLink) {
                    String parkingId = "blue_parking_link_" + link.getId().toString();

                    // create on-street parking facility
                    ActivityFacility parkingFacility = activityFacilitiesFactory.createActivityFacility(Id.create(parkingId, ActivityFacility.class),
                            link.getCoord(), link.getId());

                    // add parking activity type as an option with capacity
                    ActivityOption parkingActivityOption = activityFacilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
                    parkingActivityOption.setCapacity(random.nextInt((maxOnStreetParkingCapacity - minOnStreetParkingCapacity) + 1) +
                            minOnStreetParkingCapacity);
                    parkingFacility.addActivityOption(parkingActivityOption);

                    // add parking facility type as an option to allow future filtering
                    ActivityOption parkingFacilityTypeOption = activityFacilitiesFactory.createActivityOption(ParkingFacilityType.BlueZone.toString());
                    parkingFacility.addActivityOption(parkingFacilityTypeOption);

                    // add facility attributes (type, allowed vehicles)
                    parkingFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.BlueZone.toString());

                    // add facility to facilities
                    activityFacilities.addActivityFacility(parkingFacility);
                }

                if (random.nextDouble() < probabilityGarageParkingOnLink) {
                    String parkingId = "garage_link_" + link.getId().toString();

                    // create parking garage facility
                    ActivityFacility parkingFacility = activityFacilitiesFactory.createActivityFacility(Id.create(parkingId, ActivityFacility.class),
                            link.getCoord(), link.getId());

                    // add parking activity type as an option with capacity
                    ActivityOption parkingActivityOption = activityFacilitiesFactory.createActivityOption(ParkingUtils.PARKACTIVITYTYPE);
                    parkingActivityOption.setCapacity(random.nextInt((maxGarageParkingCapacity - minGarageParkingCapacity) + 1) +
                            minGarageParkingCapacity);
                    parkingFacility.addActivityOption(parkingActivityOption);

                    // add parking facility type as an option to allow future filtering
                    ActivityOption parkingFacilityTypeOption = activityFacilitiesFactory.createActivityOption(ParkingFacilityType.Garage.toString());
                    parkingFacility.addActivityOption(parkingFacilityTypeOption);

                    // add facility attributes (type, allowed vehicles)
                    parkingFacility.getAttributes().putAttribute("parkingFacilityType", ParkingFacilityType.Garage.toString());

                    // add facility to facilities
                    activityFacilities.addActivityFacility(parkingFacility);
                }
            }
        }
    }
}
